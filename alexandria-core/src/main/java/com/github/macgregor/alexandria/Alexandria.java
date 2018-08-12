package com.github.macgregor.alexandria;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Alexandria {

    public static void index(AlexandriaConfig config) throws IOException {
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

        for(File f : unindexed) {
            AlexandriaConfig.DocumentMetadata metadata = new AlexandriaConfig.DocumentMetadata();
            metadata.sourcePath(f.toPath());
            metadata.title(f.getName());
            config.metadata().get().add(metadata);
        }

        AlexandriaConfig.save(config);
    }

    public static void sync(AlexandriaConfig config){

    }
}
