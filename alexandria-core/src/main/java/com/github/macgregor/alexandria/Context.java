package com.github.macgregor.alexandria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

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
}
