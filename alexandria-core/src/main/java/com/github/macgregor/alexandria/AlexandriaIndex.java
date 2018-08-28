package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Process files into metadata for tracking.
 *
 * Currently has two main purposes:
 * <ol>
 *  <li>identifying un-indexed files and adding them to {@link Context#config}.</li>
 *  <li>identifying missing files to mark documents for deletion later when syncing with the remote.</li>
 * </ol>
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaIndex {
    @NonNull private Context context;

    /**
     * Entry method for {@link AlexandriaIndex#findUnindexedFiles()} and {@link AlexandriaIndex#markFilesForDeletion()}.
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown during document processing.
     */
    public void update() throws AlexandriaException {
        context.makePathsAbsolute();
        findUnindexedFiles();
        markFilesForDeletion();
    }

    /**
     * Find files on the {@link Context#searchPath} that are not already indexed.
     *
     * Newly indexed files will only have their {@link com.github.macgregor.alexandria.Config.DocumentMetadata#sourcePath}
     * and {@link com.github.macgregor.alexandria.Config.DocumentMetadata#title} set.
     *
     * The index will be saved after all matches are processed before throwing any exceptions that may have occurred
     * during batch processing.
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown during document processing.
     */
    public void findUnindexedFiles() throws AlexandriaException{
        log.debug("Looking for un-indexed files.");

        BatchProcess<Path> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> {
            Collection<Path> alreadyIndexed = documentsAlreadyIndexed(context);
            Collection<Path> matchedDocuments = documentsMatched(context);
            Collection<Path> unindexed = documentsNotIndexed(matchedDocuments, alreadyIndexed);

            log.info(String.format("Found %d un-indexed files (%d matched, %d already indexed)",
                    unindexed.size(), matchedDocuments.size(), alreadyIndexed.size()));
            return unindexed;
        }, (context, path) -> {
            log.debug("Creating metadata for unindexed file " + path.toString());
            Config.DocumentMetadata metadata = new Config.DocumentMetadata();
            metadata.sourcePath(path);
            metadata.title(path.toFile().getName());
            context.addMetadata(metadata);
        });
    }

    /**
     * Find files in the {@link Config#metadata} index that are not found on the {@link Context#searchPath}.
     *
     * A field is set in the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps} called "delete"
     * which the sync process is aware of. Once deleted on the remote, this extra property is removed and the
     * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn} field is set. The metadata itself will
     * remain unless manually deleted.
     *
     * This means you should be careful deleting files you want to remain on the remote. In this situation you can
     * manually set the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn} field or simply remove
     * both the file and metadata (if you just remove metadata it will reappear the next time indexing runs).
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown during document processing.
     */
    public void markFilesForDeletion() throws AlexandriaException{
        log.debug("Marking files for deletion.");

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> {
            Collection<Path> alreadyIndexed = documentsAlreadyIndexed(context);
            Collection<Path> matchedDocuments = documentsMatched(context);
            Collection<Path> missing = documentsIndexedButMissing(matchedDocuments, alreadyIndexed);

            log.info(String.format("Marking %d files for deletion (%d matched, %d already indexed)",
                    missing.size(), matchedDocuments.size(), alreadyIndexed.size()));

            return context.config().metadata().get()
                    .stream()
                    .filter(m -> missing.contains(m.sourcePath()))
                    .collect(Collectors.toList());
        }, (context, metadata) -> {
            log.debug(String.format("Marking %s for deletion.", metadata.sourcePath().toFile().getName()));
            metadata.setExtraProperty("delete", "true");
        });
    }

    /**
     * Find all files on {@link Context#searchPath} that match the configured include and exclude patterns.
     *
     * @see com.github.macgregor.alexandria.Resources.PathFinder
     *
     * @param context  Alexandria context containing information necessary to match files
     * @return  All matching files resolved as absolute using {@link Context#configPath}
     * @throws AlexandriaException  wrapper for any IOException thrown during conversion to make it integrate with {@link BatchProcess}
     */
    protected static Collection<Path> documentsMatched(Context context) throws AlexandriaException {
        try {
            return Resources.absolutePath(context.configPath().getParent(),
                    new Resources.PathFinder()
                            .startingInPaths(context.searchPath())
                            .including(context.include())
                            .excluding(context.exclude())
                            .paths());
        } catch(Exception e){
            throw new AlexandriaException.Builder()
                    .causedBy(e)
                    .withMessage("Problem with some of all search paths. Make sure they are all valid directories that exist.")
                    .build();
        }
    }

    /**
     * Converts {@link Config#metadata} documents into their source file paths for comparison to matched documents.
     *
     * @param context  Alexandria context containing metadata index
     * @return  Document metadata paths resolved as absolute using {@link Context#configPath}
     */
    protected static Collection<Path> documentsAlreadyIndexed(Context context){
        return Resources.absolutePath(context.configPath(),
                context.config()
                        .metadata().get()
                        .stream()
                        .map(Config.DocumentMetadata::sourcePath)
                        .collect(Collectors.toList()));
    }

    /**
     * Compare to sets of paths to determine which have not been indexed.
     *
     * Both sets of Paths should have the same bases or they wont equate properly.
     *
     * @param documentsMatched  Collection of matched documents.
     * @param documentsAlreadyIndexed  Collection of metadata paths.
     * @return  List of all paths not already indexed, or empty list if no new documents need to be indexed.
     */
    protected static Collection<Path> documentsNotIndexed(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsMatched.stream()
                .filter(p -> !documentsAlreadyIndexed.contains(p))
                .collect(Collectors.toList());
    }

    /**
     * Compare to sets of paths to determine which are missing and need to be deleted.
     *
     * Both sets of Paths should have the same bases or they wont equate properly.
     *
     * @param documentsMatched  Collection of matched documents.
     * @param documentsAlreadyIndexed  Collection of metadata paths.
     * @return  List of all paths missing on file system, or empty list if no documents are missing.
     */
    protected static Collection<Path> documentsIndexedButMissing(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsAlreadyIndexed.stream()
                .filter(p -> !documentsMatched.contains(p))
                .collect(Collectors.toList());
    }
}
