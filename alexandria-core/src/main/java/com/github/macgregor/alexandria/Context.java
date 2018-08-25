package com.github.macgregor.alexandria;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;

@Data
@Slf4j
@Accessors(fluent = true)
@AllArgsConstructor @NoArgsConstructor
public class Context {

    @NonNull private Path configPath;
    @NonNull private Path projectBase;
    @NonNull private List<String> searchPath;
    private Optional<String> outputPath = Optional.empty();
    private List<String> include = new ArrayList<>(Arrays.asList("*.md"));
    private List<String> exclude = new ArrayList<>();
    @NonNull private Config config;
    private Map<Config.DocumentMetadata, Path> convertedPaths = new HashMap<>();

    public Optional<Path> convertedPath(Config.DocumentMetadata metadata){
        return convertedPaths.containsKey(metadata) ? Optional.of(convertedPaths.get(metadata)) : Optional.empty();
    }

    public void convertedPath(Config.DocumentMetadata metadata, Path path){
        convertedPaths.put(metadata, path);
    }
}
