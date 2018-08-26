package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Core class for saving, loading and processing documents.
 * <p/>
 * The basic usage is load the state, perform operation: index, convert, sync; generally in that order,
 * save state if necessary, though the operations should handle that for you. An extra save wont hurt.
 * <p/>
 * {@code
 * Alexandria alexandria = Alexandria.loadContext(".alexandria");
 * alexandria.index()
 *      .convert()
 *      .syncWithRemote();
 * alexandria.save();
 * }
 *
 * @see Context
 * @see Config
 * @see com.github.macgregor.alexandria.remotes.Remote
 */
@Slf4j
@ToString
@Getter @Setter
@Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class Alexandria {

    @NonNull private Context context;

    /**
     * Load {@link Config} from the given file path and initialize Alexandria's {@link Context}.
     * <p>
     * Required context paths will be set to the directory of {@code filePath} and should be appropriately
     * overridden before performing any operations. If the path doesnt exist, a blank {@link Config}
     * will be created and saved to {@code filePath} when saving.
     *
     * @param filePath  path to the config file where remote details and document metadata will be saved
     * @return  Alexandria instance with an initialized context that will be provided to operations
     * @throws IOException  problems converting strings to paths or general file loading problems
     */
    public Alexandria load(String filePath) throws IOException {
        Context context = new Context();
        Path path = Resources.path(filePath, false).toAbsolutePath();
        context.configPath(path);
        context.projectBase(path.getParent().toAbsolutePath());
        context.searchPath(Collections.singletonList(path.getParent().toAbsolutePath()));

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

    /**
     * Static accessor for {@link Alexandria#load(String)}.
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static Alexandria loadContext(String filePath) throws IOException {
        return new Alexandria().load(filePath);
    }

    /**
     * Save the current context config (metadata and remote configuration) to disk.
     * <p>
     * Not all information is saved, only the config field. See {@link Config} and {@link Context}.
     *
     * @param context  context containing configuration to save
     * @throws IOException  problems saving the file
     */
    public static void save(Context context) throws IOException {
        Jackson.yamlMapper().writeValue(context.configPath().toFile(), context.config());
        log.debug(String.format("Saved configuration to %s", context.configPath().toString()));
    }

    /**
     * Convenience method for object access to {@link Alexandria#save(Context)}.
     *
     * @return
     * @throws IOException
     */
    public Alexandria save() throws IOException {
        Alexandria.save(context);
        return this;
    }

    /**
     * Update the metadata index based on matched files found on the {@link Context#searchPath}.
     * <p>
     * Any files found that are not in the list of metadata will be created and added to the list to be
     * later converted and published. Files already in the index that are not found on the search path
     * are marked for deletion from the remote.
     * <p>
     * @see AlexandriaConvert
     *
     * @return
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria index() throws AlexandriaException {
        new AlexandriaIndex(context).update();
        return this;
    }

    /**
     * Convert HTML files from the files in the metadata index.
     * <p>
     * Converted files will be saved to {@link Context#outputPath}, if set. Otherwise the files will be converted in place
     * in the same directory as the markdown file being converted. Any exceptions thrown will be collected and thrown
     * after processing all documents.
     *
     * @return
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria convert() throws AlexandriaException {
        new AlexandriaConvert(context).convert();
        return this;
    }

    /**
     * Sync all documents with the configured remote. Creating, updating or deleting as necessary.
     * <p/>
     * <ul>
     *  <li><b>Create</b> - If the metadata has no {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * it is taken as a sign to create a new document. If the document already exists, be sure to manually set the remote
     * uri in the config file to update the exiting document.
     *  <li><b>Update</b> - A checksum of the source document (not the converted html file) is used to determine when to update the
     * remote document. If the checksum calculated at runtime is different than the checksum stored in the metadata index,
     * the remote document is updated.
     *  <li><b>Delete</b> - The index phase will mark metadata index entries that are not found on the search path for
     * deletion in the metadata's {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}. Once deleted,
     * the extra property flag is removed and the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn}
     * field is set which makes all phases essentially ignore it.
     * </ul>
     * <p>
     * @see AlexandriaIndex
     * @see AlexandriaConvert
     * @see AlexandriaSync
     *
     * @return
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria syncWithRemote() throws AlexandriaException {
        new AlexandriaSync(context).syncWithRemote();
        return this;
    }
}
