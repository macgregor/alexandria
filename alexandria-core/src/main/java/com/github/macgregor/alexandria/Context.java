package com.github.macgregor.alexandria;

import java.nio.file.Path;
import java.util.*;

public class Context {

    private Path configPath;
    private Path projectBase;
    private List<String> searchPath;
    private Optional<String> outputPath = Optional.empty();
    private List<String> include = new ArrayList<>(Arrays.asList("*.md"));
    private List<String> exclude = new ArrayList<>();
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

    public List<String> searchPath() {
        return searchPath;
    }

    public void searchPath(List<String> searchPath) {
        this.searchPath = searchPath;
    }

    public Optional<String> output() {
        return outputPath;
    }

    public void output(Optional<String> outputPath) {
        this.outputPath = outputPath;
    }

    public List<String> include() {
        return include;
    }

    public void include(List<String> include) {
        this.include = include;
    }

    public List<String> exclude() {
        return exclude;
    }

    public void exclude(List<String> exclude) {
        this.exclude = exclude;
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
