package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public class AlexandriaIndex {
    private static Logger log = LoggerFactory.getLogger(AlexandriaIndex.class);

    private Context context;

    public AlexandriaIndex(){}

    public AlexandriaIndex(Context context){
        this.context = context;
    }

    public void update() throws AlexandriaException {
        log.debug("Updating metadata index.");

        BatchProcess<Path> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> {
            Collection<Path> alreadyIndexed = documentsAlreadyIndexed(context);
            Collection<Path> matchedDocuments = documentsMatched(context);
            Collection<Path> unindexed = documentsNotIndexed(matchedDocuments, alreadyIndexed);
            Collection<Path> missing = documentsIndexedButMissing(matchedDocuments, alreadyIndexed);

            log.info(String.format("Matched %d files (%d indexed, %d already indexed, %d missing)",
                    matchedDocuments.size(), unindexed.size(), alreadyIndexed.size(), missing.size()));
            return unindexed;
        }, (context, path) -> {
            log.debug("Creating metadata for unindexed file " + path.toString());
            Config.DocumentMetadata metadata = new Config.DocumentMetadata();
            metadata.sourcePath(path);
            metadata.title(path.toFile().getName());
            context.config().metadata().get().add(metadata);
        });
    }

    protected Collection<Path> documentsMatched(Context context) throws AlexandriaException {
        try {
            return Resources.relativeTo(context.projectBase(),
                    new Resources.PathFinder()
                            .startingIn(context.searchPath())
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

    protected Collection<Path> documentsAlreadyIndexed(Context context){
        return Resources.relativeTo(context.projectBase(),
                context.config()
                        .metadata().get()
                        .stream()
                        .map(Config.DocumentMetadata::sourcePath)
                        .collect(Collectors.toList()));
    }

    protected Collection<Path> documentsNotIndexed(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsMatched.stream()
                .filter(p -> !documentsAlreadyIndexed.contains(p))
                .collect(Collectors.toList());
    }

    protected Collection<Path> documentsIndexedButMissing(Collection<Path> documentsMatched, Collection<Path> documentsAlreadyIndexed){
        return documentsAlreadyIndexed.stream()
                .filter(p -> !documentsMatched.contains(p))
                .collect(Collectors.toList());
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
