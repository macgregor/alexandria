package com.github.macgregor.alexandria;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context {

    private Path configPath;
    private Path projectBase;
    private Config config;
    private Map<Config.DocumentMetadata, Path> convertedPaths = new HashMap<>();

    public Optional<Path> convertedPath(Config.DocumentMetadata metadata){
        return convertedPaths.containsKey(metadata) ? Optional.of(convertedPaths.get(metadata)) : Optional.empty();
    }

    public void convertedPath(Config.DocumentMetadata metadata, Path path){
        convertedPaths.put(metadata, path);
    }

    public Path configPath() {
        return configPath;
    }

    public void configPath(Path configPath) {
        this.configPath = configPath;
    }

    public Path projectBase() {
        return projectBase;
    }

    public void projectBase(Path projectBase) {
        this.projectBase = projectBase;
    }

    public Config config() {
        return config;
    }

    public void config(Config config) {
        this.config = config;
    }

    public Map<Config.DocumentMetadata, Path> convertedPaths() {
        return convertedPaths;
    }

    public void convertedPaths(Map<Config.DocumentMetadata, Path> convertedPaths) {
        this.convertedPaths = convertedPaths;
    }
}
