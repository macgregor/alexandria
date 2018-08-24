package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Core class containing the high level commands to indexing metadata, converting html and
 * syncing documents with the remote. Each of these can be executed independently but form a
 * lifeycle: index -> convert -> syncWithRemote.
 */
public class Alexandria {
    private static Logger log = LoggerFactory.getLogger(Alexandria.class);

    private Context context;

    public Alexandria(){}

    public Context context() {
        return context;
    }

    public void context(Context context) {
        this.context = context;
    }

    public Alexandria load(String filePath) throws IOException {
        Context context = new Context();
        Path path = Resources.path(filePath, false).toAbsolutePath();
        context.configPath(path);
        context.projectBase(path.getParent().toAbsolutePath());

        Config config;
        if(path.toFile().exists()) {
            config = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            log.debug(String.format("Loaded configuration from %s", path.toString()));
        } else{
            config = new Config();
            log.debug(String.format("Created default configuration for new file %s", path.toString()));
        }
        context.config(config);

        this.context = context;
        return this;
    }

    public Alexandria save() throws IOException {
        Jackson.yamlMapper().writeValue(context.configPath().toFile(), context.config());
        log.debug(String.format("Saved configuration to %s", context.configPath().toString()));
        return this;
    }

    /**
     * Update the metadata index based on matched files found on the search path. Any files found that
     * are not in the list of metadata will be created and added to the list to be later converted and published.
     *
     * TODO:
     *   - files found in the index but not on the file system should be marked for deletion in the remote.
     *   - pull title from header in the source document
     *
     * @throws AlexandriaException Any exception is thrown, most likely an IOException due to a missing file or invalid path.
     */
    public Alexandria index() throws AlexandriaException {
        new AlexandriaIndex(context).update();
        return this;
    }

    /**
     * Convert HTML files from the files in the metadata index. Converted files will be saved to the configured {@link Context#outputPath}, if set.
     * Otherwise the files will be converted in place in the same directory as the markdown file being converted. Any exceptions
     * thrown will be collected and thrown after processing all documents.
     *
     * @throws BatchProcessException Exception wrapping all exceptions thrown during document processing.
     */
    public Alexandria convert() throws AlexandriaException {
        new AlexandriaConvert(context).convert();
        return this;
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
     * @throws BatchProcessException Any errors are thrown for any {@link com.github.macgregor.alexandria.Config.DocumentMetadata}
     * @throws IllegalStateException No remote is configured, the remote configuration is invalid.
     */
    public Alexandria syncWithRemote() throws AlexandriaException {
        new AlexandriaSync(context).syncWithRemote();
        return this;
    }
}
