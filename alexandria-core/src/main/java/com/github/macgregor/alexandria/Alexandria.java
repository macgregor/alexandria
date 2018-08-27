package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Core class for saving, loading and processing documents.
 *
 * The basic usage is load the state, perform operation: index, convert, sync; generally in that order,
 * save state if necessary, though the operations should handle that for you. An extra save wont hurt.
 *
 * <pre>
 * {@code
 * Alexandria alexandria = Alexandria.loadContext(".alexandria");
 * alexandria.index()
 *      .convert()
 *      .syncWithRemote();
 * alexandria.save();
 * }
 * </pre>
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
     * Update the metadata index based on matched files found on the {@link Context#searchPath}.
     *
     * Any files found that are not in the list of metadata will be created and added to the list to be
     * later converted and published. Files already in the index that are not found on the search path
     * are marked for deletion from the remote.
     *
     * @see AlexandriaConvert
     *
     * @return  Alexandria instance that with an updated metadata index
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria index() throws AlexandriaException {
        new AlexandriaIndex(context).update();
        return this;
    }

    /**
     * Convert HTML files from the files in the metadata index.
     *
     * Converted files will be saved to {@link Context#outputPath}, if set. Otherwise the files will be converted in place
     * in the same directory as the markdown file being converted. Any exceptions thrown will be collected and thrown
     * after processing all documents.
     *
     * @return  Alexandria instance with converted document file paths
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria convert() throws AlexandriaException {
        new AlexandriaConvert(context).convert();
        return this;
    }

    /**
     * Sync all documents with the configured remote. Creating, updating or deleting as necessary.
     *
     * <ul>
     *  <li><b>Create</b> - If the metadata has no {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * it is taken as a sign to create a new document. If the document already exists, be sure to manually set the remote
     * uri in the config file to update the exiting document.</li>
     *  <li><b>Update</b> - A checksum of the source document (not the converted html file) is used to determine when to update the
     * remote document. If the checksum calculated at runtime is different than the checksum stored in the metadata index,
     * the remote document is updated.</li>
     *  <li><b>Delete</b> - The index phase will mark metadata index entries that are not found on the search path for
     * deletion in the metadata's {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}. Once deleted,
     * the extra property flag is removed and the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn}
     * field is set which makes all phases essentially ignore it.</li>
     * </ul>
     *
     * @see AlexandriaIndex
     * @see AlexandriaConvert
     * @see AlexandriaSync
     *
     * @return  Alexandria instance with updated metadata after the sync
     * @throws AlexandriaException  wrapper for any exceptions thrown processing the documents
     */
    public Alexandria syncWithRemote() throws AlexandriaException {
        new AlexandriaSync(context).syncWithRemote();
        return this;
    }
}
