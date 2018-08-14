package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.HttpException;
import com.github.macgregor.alexandria.remotes.JiveRemote;
import com.github.macgregor.alexandria.remotes.Remote;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Alexandria {

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
        } catch(Exception e){
            throw new AlexandriaException.Builder()
                    .withMessage("Unexpeccted exception generating local metadata index")
                    .causedBy(e)
                    .build();
        }
    }

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

        if(exceptions.size() > 0){
            throw new BatchProcessException.Builder()
                    .withMessage(String.format("Failed to convert %d out of %d documents to html", exceptions.size(), config.metadata().get().size()))
                    .causedBy(exceptions)
                    .build();
        }
    }

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
