package com.github.macgregor.alexandria;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    }

    public static void generate(AlexandriaConfig config) throws IOException {
        if(config.remote().isPresent() && config.remote().get().supportsNativeMarkdown().get()){
            return;
        }
        for(AlexandriaConfig.DocumentMetadata metadata : config.metadata().get()){
            String convertedDir = config.output().orElse(metadata.sourcePath().getParent().toString());
            String convertedFileName = FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()) + ".html";
            metadata.convertedPath(Optional.of(Paths.get(convertedDir, convertedFileName)));
            Markdown.toHtml(metadata);
        }
    }
}
