package com.github.macgregor.alexandria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Data
@Slf4j
@Accessors(fluent = true)
@AllArgsConstructor @NoArgsConstructor
public class Context {

    @NonNull private Path configPath;
    @NonNull private Path projectBase;
    @NonNull private List<Path> searchPath;
    private Optional<Path> outputPath = Optional.empty();
    private List<String> include = new ArrayList<>(Arrays.asList("*.md"));
    private List<String> exclude = new ArrayList<>();
    @NonNull private Config config = new Config();
    private Map<Config.DocumentMetadata, Path> convertedPaths = new HashMap<>();

    public Path resolveRelativePath(Path relativePath){
        return projectBase.resolve(relativePath);
    }

    public int documentCount(){
        return config.metadata().isPresent() ? config.metadata().get().size() : 0;
    }

    public Optional<Path> convertedPath(Config.DocumentMetadata metadata){
        return convertedPaths.containsKey(metadata) ? Optional.of(convertedPaths.get(metadata)) : Optional.empty();
    }

    public void convertedPath(Config.DocumentMetadata metadata, Path path){
        convertedPaths.put(metadata, path);
    }

    public static Context load(String filePath) throws IOException {
        Context context = new Context();
        Path path = Resources.path(filePath, false).toAbsolutePath();
        context.configPath(path);
        context.projectBase(path.getParent().toAbsolutePath());

        Config config;
        if(path.toFile().exists()) {
            config = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            log.debug(String.format("Loaded configuration from %s", path.toString()));
        } else{
            config = new Config();
            log.debug(String.format("Created default configuration for new file %s", path.toString()));
        }
        context.config(config);
        return context;
    }

    public static void save(Context context) throws IOException {
        Jackson.yamlMapper().writeValue(context.configPath().toFile(), context.config());
        log.debug(String.format("Saved configuration to %s", context.configPath().toString()));
    }
}
