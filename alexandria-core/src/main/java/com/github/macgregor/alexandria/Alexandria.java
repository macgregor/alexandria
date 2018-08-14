package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.HttpException;
import com.github.macgregor.alexandria.remotes.JiveRemote;
import com.github.macgregor.alexandria.remotes.Remote;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core class containing the high level commands to indexing metadata, converting html and
 * syncing documents with the remote. Each of these can be executed independently but form a
 * lifeycle: index -> generate -> syncWithRemote.
 */
public class Alexandria {

    /**
     * Update the metadata index based on matched files found on the search path. Any files found that
     * are not in the list of metadata will be created and added to the list to be later converted and published.
     *
     * TODO:
     *   - files found in the index but not on the file system should be marked for deletion in the remote.
     *   - pull title from header in the source document
     *
     * @param config
     * @throws AlexandriaException Any exception is thrown, most likely an IOException due to a missing file or invalid path.
     */
    public static void index(Config config) throws AlexandriaException {
        try {
            Collection<File> matchedDocuments = new Resources.PathFinder()
                    .startingIn(config.searchPath())
                    .including(config.include())
                    .excluding(config.exclude().get())
                    .find();

            List<File> alreadyIndexed = config.metadata().get().stream()
                    .map(m -> m.sourcePath())
                    .map(p -> p.toFile())
                    .collect(Collectors.toList());

            List<File> unindexed = matchedDocuments.stream()
                    .filter(f -> !alreadyIndexed.contains(f))
                    .collect(Collectors.toList());

            for (File f : unindexed) {
                Config.DocumentMetadata metadata = new Config.DocumentMetadata();
                metadata.sourcePath(f.toPath());
                metadata.title(f.getName());
                config.metadata().get().add(metadata);
            }
            Config.save(config);
        } catch(Exception e){
            throw new AlexandriaException.Builder()
                    .withMessage("Unexpeccted exception generating local metadata index")
                    .causedBy(e)
                    .build();
        }
    }

    /**
     * Generate HTML files from the metadata index. Generated files will be saved to the configured {@link Config#outputPath}, if set.
     * Otherwise the files will be generated in place in the same directory as the markdown file being converted. Any exceptions
     * thrown will be collected and thrown after processing all documents
     *
     * @param config
     * @throws BatchProcessException Exception wrapping all exceptions thrown during document processing.
     */
    public static void generate(Config config) throws BatchProcessException {
        if(config.remote().isPresent() &&
                config.remote().get().supportsNativeMarkdown().isPresent() &&
                config.remote().get().supportsNativeMarkdown().get()){
            return;
        }

        List<AlexandriaException> exceptions = new ArrayList<>();

        for(Config.DocumentMetadata metadata : config.metadata().get()){
            try {
                String convertedDir = config.output().orElse(metadata.sourcePath().getParent().toString());
                String convertedFileName = FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()) + ".html";
                metadata.convertedPath(Optional.of(Paths.get(convertedDir, convertedFileName)));
                Markdown.toHtml(metadata);
            } catch(Exception e){
                exceptions.add(new AlexandriaException.Builder()
                        .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                        .causedBy(e)
                        .metadataContext(metadata)
                        .build());
            }
        }

        try {
            Config.save(config);
        } catch (IOException e) {
            exceptions.add(new AlexandriaException.Builder()
                    .withMessage(String.format("Unable to save configuration to %s", config.configPath()))
                    .causedBy(e)
                    .build());
        }

        if(exceptions.size() > 0){
            throw new BatchProcessException.Builder()
                    .withMessage(String.format("Failed to convert %d out of %d documents to html", exceptions.size(), config.metadata().get().size()))
                    .causedBy(exceptions)
                    .build();
        }
    }

    /**
     * Sync all documents with the configured remote. All exceptions thrown during processing will be collected and thrown
     * as a single {@link BatchProcessException} at the end of processing.
     *
     * Create - If the metadata has no {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * it is taken as a sign to create a new document.If the document already exists, be sure to manually set the remote
     * uri in the config file to update the exiting document.
     *
     * Update - A checksum of the source document (not the converted html file) is used to determine when to update the
     * remote document. If the checksum calculated at runtime is different than the checksum stored in the metadata index,
     * the remote document is updated.
     *
     * Delete - not implemented yet.
     *
     * @param config
     * @throws BatchProcessException Any errors are thrown for any {@link com.github.macgregor.alexandria.Config.DocumentMetadata}
     * @throws IllegalStateException No remote is configured, the remote configuration is invalid.
     */
    public static void syncWithRemote(Config config) throws BatchProcessException {
        if(!config.remote().isPresent()){
            throw new IllegalStateException("No configured remote.");
        }
        Remote remote = new JiveRemote(config.remote().get());
        remote.validateRemoteConfig();

        List<AlexandriaException> exceptions = new ArrayList<>();

        for(Config.DocumentMetadata metadata : config.metadata().get()){
            try {
                remote.validateDocumentMetadata(metadata);
                long currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
                if (!metadata.remoteUri().isPresent()) {
                    remote.create(metadata);
                } else {
                    if (!metadata.sourceChecksum().isPresent() || !metadata.sourceChecksum().get().equals(currentChecksum)) {
                        remote.update(metadata);
                    }
                }
                metadata.sourceChecksum(Optional.of(currentChecksum));
            } catch(HttpException e){
                exceptions.add(e);
            } catch(Exception e){
                exceptions.add(new AlexandriaException.Builder()
                        .withMessage(String.format("Unexcepted error syncing %s to remote", metadata.sourcePath()))
                        .causedBy(e)
                        .metadataContext(metadata)
                        .build());
            }
        }

        if(exceptions.size() > 0){
            throw new BatchProcessException.Builder()
                    .withMessage(String.format("Failed to sync %d out of %d documents to remote", exceptions.size(), config.metadata().get().size()))
                    .causedBy(exceptions)
                    .build();
        }
    }
}
