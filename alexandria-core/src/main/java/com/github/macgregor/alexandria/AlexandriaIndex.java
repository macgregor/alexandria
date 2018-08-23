package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AlexandriaIndex {
    private static Logger log = LoggerFactory.getLogger(AlexandriaIndex.class);

    private Context context;
    private List<AlexandriaException> exceptions = new ArrayList<>();

    public AlexandriaIndex(){}

    public AlexandriaIndex(Context context){
        this.context = context;
    }

    public void update() throws BatchProcessException {
        log.debug("Updating metadata index.");
        try {
            Collection<Path> alreadyIndexed = documentsAlreadyIndexed();
            Collection<Path> matchedDocuments = documentsMatched();
            Collection<Path> unindexed = documentsNotIndexed(matchedDocuments, alreadyIndexed);
            Collection<Path> missing = documentsIndexedButMissing(matchedDocuments, alreadyIndexed);

            log.debug(String.format("Found %d unindexed files.", unindexed.size()));
            for (Path p : unindexed) {
                try {
                    log.debug("Creating metadata for unindexed file " + p.toString());
                    Config.DocumentMetadata metadata = new Config.DocumentMetadata();
                    metadata.sourcePath(p);
                    metadata.title(p.toFile().getName());
                    context.config().metadata().get().add(metadata);
                } catch(Exception e){
                    exceptions.add(new AlexandriaException.Builder()
                            .withMessage(String.format("Unexpected exception thrown indexing %s.", p.toString()))
                            .causedBy(e)
                            .build());
                }
            }

            log.info(String.format("Matched %d files (%d indexed, %d already indexed, %d missing)",
                    matchedDocuments.size(), unindexed.size(), alreadyIndexed.size(), missing.size()));
        } catch(Exception e){
            exceptions.add(new AlexandriaException.Builder()
                    .withMessage("Error searching for documents to index.")
                    .causedBy(e)
                    .build());
        }

        if(exceptions.size() > 0){
            throw new BatchProcessException.Builder()
                    .withMessage("Alexandria Index has errors.")
                    .causedBy(exceptions)
                    .build();
        }
    }

    protected Collection<Path> documentsMatched() throws IOException {
        return Resources.relativeTo( context.projectBase(),
                new Resources.PathFinder()
                        .startingIn(context.searchPath())
                        .including(context.include())
                        .excluding(context.exclude())
                        .paths());
    }

    protected Collection<Path> documentsAlreadyIndexed(){
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

    public List<AlexandriaException> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<AlexandriaException> exceptions) {
        this.exceptions = exceptions;
    }
}
