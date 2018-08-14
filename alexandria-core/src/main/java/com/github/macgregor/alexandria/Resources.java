package com.github.macgregor.alexandria;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for working with file system resources.
 */
public class Resources {

    /**
     * Builder to find files among directories matching based on include/exclude patterns. Makes use of
     * {@link FileUtils#listFiles(File, IOFileFilter, IOFileFilter)} to do the heavy lifting.
     */
    public static class PathFinder{

        private List<Path> startingDirs;
        private List<String> include;
        private List<String> exclude;
        private boolean recursive;

        public PathFinder(){
            startingDirs = Collections.emptyList();
            include = Collections.singletonList("*");
            exclude = Collections.emptyList();
            recursive = true;
        }

        public PathFinder startingIn(String dir) throws IOException {
            return startingIn(Collections.singletonList(dir));
        }

        /**
         * Converts the provided strings into {@link Path} objects and validates they are directories that exist.
         * @param dirs
         * @return
         * @throws IOException Any path doesnt exist or is not a directory.
         */
        public PathFinder startingIn(List<String> dirs) throws IOException {
            List<Path> paths = new ArrayList<>();
            for(String dir : dirs){
                Path dirPath = Paths.get(dir);
                if(!Files.exists(dirPath)){
                    throw new IOException(String.format("Directory %s doesnt exist.", dir));
                }
                if(!Files.isDirectory(dirPath)){
                    throw new IOException(String.format("%s is not a directory.", dir));
                }
                paths.add(dirPath);
            }
            this.startingDirs = paths;
            return this;
        }

        /**
         * Wild card include pattern, see {@link WildcardFileFilter}.
         * @param include Wildcard filename patterns, e.g. *.md
         * @return
         */
        public PathFinder including(List<String> include){
            this.include = include;
            return this;
        }

        /**
         * Wild card include pattern, see {@link WildcardFileFilter}.
         * @param include Wildcard filename pattern, e.g. *.md
         * @return
         */
        public PathFinder including(String include){
            this.include = Collections.singletonList(include);
            return this;
        }

        /**
         * Wild card include pattern, see {@link WildcardFileFilter}.
         * @param exclude Wildcard filename patterns, e.g. *.md
         * @return
         */
        public PathFinder excluding(List<String> exclude){
            this.exclude = exclude;
            return this;
        }

        /**
         * Wild card include pattern, see {@link WildcardFileFilter}.
         * @param exclude Wildcard filename pattern, e.g. *.md
         * @return
         */
        public PathFinder excluding(String exclude){
            this.exclude = Collections.singletonList(exclude);
            return this;
        }

        /**
         * When set, recursively walk directories when finding files. Defaults to true.
         *
         * @param isRecursive
         * @return
         */
        public PathFinder recursive(boolean isRecursive){
            this.recursive = isRecursive;
            return this;
        }

        /**
         * Find all files using the builder properties. Files will only be included if they match any
         * of the include patterns and non of the exclude filters.
         * @return
         */
        public Collection<File> find(){
            IOFileFilter dirFilter = recursive ? TrueFileFilter.INSTANCE : null;
            IOFileFilter fileFilter = new AndFileFilter(
                    new WildcardFileFilter(include),
                    new NotFileFilter(new WildcardFileFilter(exclude)));

            return startingDirs.stream()
                    .map(d -> FileUtils.listFiles(d.toFile(), fileFilter, dirFilter))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
    }

    public static void save(String filePath, String content) throws IOException {
        Resources.save(filePath, content, true);
    }

    /**
     * Save the string content to a file at the provided file path, optionally overwriting the file if it exists. If
     * the file path exists and is a directory or overwrite is false, an exception will be thrown, otherwise the file
     * will be deleted before being recreated with the new content.
     *
     * @param filePath Path to the file to save. File path cannot be to an existing directory.
     * @param content File contents to write.
     * @param overwrite Whether or not to overwrite the file if it already exists.
     * @throws IOException If the file path cant be overwritten, the file path is invalid or an general IO error occurred.
     */
    public static void save(String filePath, String content, boolean overwrite) throws IOException {
        Path path = Paths.get(filePath);
        if(Files.exists(path)){
            if(Files.isDirectory(path)){
                throw new FileAlreadyExistsException("File is a directory. Refusing to destroy.");
            } else if(!overwrite) {
                throw new FileAlreadyExistsException("Refusing to overwrite existing file.");
            }
        }
        FileUtils.writeStringToFile(path.toFile(), content, (String) null);
    }

    public static String load(String filePath) throws IOException {
        return FileUtils.readFileToString(Paths.get(filePath).toFile(), (String) null);
    }

    public static Set<Path> paths(Collection<String>rawPaths) {
        try {
            return paths(rawPaths, false, false, false);
        } catch(FileNotFoundException e){
            throw new RuntimeException("This exception should not have happened, theres a bug in Resources.paths(Collection<String>, boolean, boolean, boolean)");
        }
    }

    public static Set<Path> paths(Collection<String>rawPaths, boolean failOnNonExistantPath) throws FileNotFoundException {
        return paths(rawPaths, failOnNonExistantPath, false, false);
    }

    public static Set<Path> filePaths(Collection<String> rawPaths, boolean failOnNonExistantPath) throws FileNotFoundException {
        return paths(rawPaths, failOnNonExistantPath, false, true);
    }

    public static Set<Path> directoryPaths(Collection<String> rawPaths, boolean failOnNonExistantPath) throws FileNotFoundException {
        return paths(rawPaths, failOnNonExistantPath, true, false);
    }

    /**
     * Convert the collection of raw path strings into a set of parsed Path objects. Optionally fail if the path doesnt
     * exist, ignore files and ignore directories.
     *
     * @param rawPaths
     * @param failOnNonExistantPath
     * @param ignoreFiles
     * @param ignoreDirectories
     * @return
     * @throws FileNotFoundException
     */
    public static Set<Path> paths(Collection<String> rawPaths, boolean failOnNonExistantPath, boolean ignoreFiles, boolean ignoreDirectories) throws FileNotFoundException {
        Set<Path> convertedPaths = new HashSet<>(rawPaths.size());
        for(String rawPath : rawPaths){
            Path p = Paths.get(rawPath);
            if(Files.exists(p)){
                if(Files.isRegularFile(p) && !ignoreFiles){
                    convertedPaths.add(p);
                } else if(Files.isDirectory(p) && !ignoreDirectories){
                    convertedPaths.add(p);
                }
            } else{
                if(failOnNonExistantPath){
                    throw new FileNotFoundException(String.format("File %s doesnt exist", rawPath));
                }
                convertedPaths.add(p);
            }
        }
        return convertedPaths;
    }

    public static Path path(String rawPath){
        try {
            return path(rawPath, false);
        } catch(FileNotFoundException e){
            throw new RuntimeException("This exception should not have happened, theres a bug in Resources.path(String, boolean)");
        }
    }

    public static Path path(String rawPath, boolean failOnNonExistantPath) throws FileNotFoundException {
        Path p = Paths.get(rawPath);
        if(failOnNonExistantPath && !Files.exists(p)){
            throw new FileNotFoundException(String.format("File %s doesnt exist", rawPath));
        }
        return p;
    }
}
