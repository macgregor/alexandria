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

/**
 * Runtime context containing arguments passed from the user agent (e.g. search path, files to include or exclude, etc).
 *
 * This information is not persisted by Alexandria as it is considered specific to the runtime environment at execution
 * that is not necessarily suitable distribution to others. Absolute paths are a good example of things that would
 * break others if they were committed to git.
 *
 * Any paths in the context should be absolute, while paths in {@link Config} are relative.
 */
@Data
@Slf4j
@Accessors(fluent = true)
@AllArgsConstructor @NoArgsConstructor
public class Context {

    /** Absolute path to the Alexandria {@link Config} file to load/save to. */
    @NonNull private Path configPath;

    /** Absolute path to the project base where Alexandria is being executed */
    @NonNull private Path projectBase;

    /** Absolute paths to use when searching for documents to index  */
    @NonNull private List<Path> searchPath;

    /** Absolute path to output converted documents to. If not set, documents are converted in place where they are found. Default: none. */
    private Optional<Path> outputPath = Optional.empty();

    /** File include patterns to use when searching for documents to index. See {@link Resources.PathFinder}. Default: *.md */
    private List<String> include = new ArrayList<>(Arrays.asList("*.md"));

    /** File exclude patterns to use when searching for documents to index. See {@link Resources.PathFinder}. Default: none */
    private List<String> exclude = new ArrayList<>();

    /** Aleandria config containing document index and remote config. */
    @NonNull private Config config = new Config();

    /** Alexandria config originally loaded from the file system. */
    @NonNull private Config originalConfig = new Config();

    /** Cache for tracking absolute converted file paths for indexed metadata. Default: empty map. */
    private Map<Config.DocumentMetadata, Path> convertedPaths = new HashMap<>();

    /**
     * Resolve a relative path to an absolute one using the {@link #configPath} directory. Useful for resolving relative
     * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#sourcePath}.
     *
     * @param relativePath  relative path to convert to absolute
     * @return  the absolute path
     */
    public Path resolveRelativePath(Path relativePath){
        return configPath.getParent().resolve(relativePath);
    }

    /**
     * Convenience method for determining the size of the documentation index.
     *
     * @return {@link Config#metadata} size if present, otherwise 0
     */
    public int documentCount(){
        return config.metadata().isPresent() ? config.metadata().get().size() : 0;
    }

    /**
     * Convenience method to retrieve the absolute converted file path in the cache for the given document.
     *
     * @param metadata  indexed document to look for
     * @return  The converted path if it exist in the cache, otherwise Optional.empty()
     */
    public Optional<Path> convertedPath(Config.DocumentMetadata metadata){
        return convertedPaths.containsKey(metadata) ? Optional.of(convertedPaths.get(metadata)) : Optional.empty();
    }

    /**
     * Convenience method to add a absolute converted file path to the cache.
     *
     * @param metadata  indexed metadata that has been converted.
     * @param path  absolute path to the converted html file for the document.
     */
    public void convertedPath(Config.DocumentMetadata metadata, Path path){
        convertedPaths.put(metadata, path);
    }

    /**
     * Initialize Alexandria's {@link Context}, loading the {@link Config} from the given file path.
     *
     * Required context paths will be set to the directory of {@code filePath} and should be appropriately
     * overridden before performing any operations. If the path doesnt exist, a blank {@link Config}
     * will be created and saved to {@code filePath} when saving.
     *
     * @param filePath  path to the config file where remote details and document metadata will be saved
     * @return  Initialized Alexandria context instance that will be provided to operations
     * @throws IOException  problems converting strings to paths or general file loading problems
     */
    public static Context load(String filePath) throws IOException {
        Context context = new Context();
        Path path = Resources.path(filePath, false).toAbsolutePath();
        context.configPath(path);
        context.projectBase(path.getParent().toAbsolutePath());
        context.searchPath(Collections.singletonList(path.getParent().toAbsolutePath()));

        if(path.toFile().exists()) {
            Config originalConfig = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            Config config = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            context.config(config);
            context.originalConfig(originalConfig);
            log.debug(String.format("Loaded configuration from %s", path.toString()));
        } else{
            log.debug(String.format("Created default configuration for new file %s", path.toString()));
        }

        return context;
    }

    /**
     * Save the current context config (metadata and remote configuration) to disk.
     *
     * Not all information is saved, only the config field. See {@link Config} and {@link Context}.
     *
     * @param context  context containing configuration to save
     * @throws IOException  problems saving the file
     */
    public static void save(Context context) throws IOException {
        Config toSave = context.originalConfig;
        toSave.metadata(context.config.metadata());

        Jackson.yamlMapper().writeValue(context.configPath().toFile(), toSave);
        log.debug(String.format("Saved configuration to %s", context.configPath().toString()));
    }
}
