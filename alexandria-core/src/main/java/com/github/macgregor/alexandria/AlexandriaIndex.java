package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaIndex {
    @NonNull private Context context;

    public void update() throws AlexandriaException {
        findUnindexedFiles();
        markFilesForDeletion();
    }

    public void findUnindexedFiles() throws AlexandriaException{
        log.debug("Looking for unindexed files.");

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
            context.config().metadata().get().add(metadata);
        });
    }

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
                    .filter(m -> missing.contains(context.projectBase().relativize(m.sourcePath())))
                    .collect(Collectors.toList());
        }, (context, metadata) -> {
            log.debug(String.format("Marking %s for deletion.", metadata.sourcePath().toFile().getName()));
            if(!metadata.extraProps().isPresent()){
                metadata.extraProps(Optional.of(new HashMap<>()));
            }
            metadata.extraProps().get().put("delete", "true");
        });
    }

    protected static Collection<Path> documentsMatched(Context context) throws AlexandriaException {
        try {
            return Resources.relativeTo(context.projectBase(),
                    new Resources.PathFinder()
                            .startingInPaths(context.searchPath())
                            .including(context.include())
                            .excluding(context.exclude())
                            .paths());
        } catch(IOException e){
            throw new AlexandriaException.Builder()
                    .causedBy(e)
                    .withMessage("Problem with some of all search paths. Make sure they are all valid directories that exist.")
                    .build();
        }
    }

    protected static Collection<Path> documentsAlreadyIndexed(Context context){
        return Resources.relativeTo(context.projectBase(),
                context.config()
                        .metadata().get()
                        .stream()
                        .map(Config.DocumentMetadata::sourcePath)
                        .collect(Collectors.toList()));
    }

    protected static Collection<Path> documentsNotIndexed(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsMatched.stream()
                .filter(p -> !documentsAlreadyIndexed.contains(p))
                .collect(Collectors.toList());
    }

    protected static Collection<Path> documentsIndexedButMissing(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsAlreadyIndexed.stream()
                .filter(p -> !documentsMatched.contains(p))
                .collect(Collectors.toList());
    }
}
