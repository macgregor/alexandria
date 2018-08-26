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
 * <p>
 * Find files/paths/directories that match patterns, saving and loading files, etc.
 */
public class Resources {

    /**
     * Find files along a set of directories that based on include/exclude patterns.
     * <p>
     * Makes use of {@link FileUtils#listFiles(File, IOFileFilter, IOFileFilter)} to do most of the work.
     */
    public static class PathFinder{

        private Collection<Path> startingDirs;
        private List<String> include;
        private List<String> exclude;
        private boolean recursive;

        public PathFinder(){
            startingDirs = Collections.emptyList();
            include = Collections.singletonList("*");
            exclude = Collections.emptyList();
            recursive = true;
        }

        /**
         * Set starting directory to a single directory, converting it to a {@link Path}
         * <p>
         * Path will be validated to be a directory that exists
         *
         * @param dir
         * @return
         * @throws IOException  The path doesnt exist or is not a directory.
         */
        public PathFinder startingIn(String dir) throws IOException {
            return startingIn(Collections.singletonList(dir));
        }

        /**
         * Set the starting directories, converting the provided string paths into {@link Path} objects.
         * <p>
         * Each path will be validated to be a directory that exists
         *
         * @param dirs
         * @return
         * @throws IOException Any path doesnt exist or is not a directory.
         */
        public PathFinder startingIn(Collection<String> dirs) throws IOException {
            List<Path> paths = new ArrayList<>();
            for(String dir : dirs){
                Path dirPath = Paths.get(dir);
                paths.add(dirPath);
            }
            return startingInPaths(paths);
        }

        /**
         * Sets the starting directory to a single directory.
         * <p>
         * Path will be validated to be a directory that exists
         *
         * @param dir
         * @return
         * @throws IOException  The path doesnt exist or is not a directory.
         */
        public PathFinder startingInPath(Path dir) throws IOException {
            return startingInPaths(Collections.singletonList(dir));
        }

        /**
         * Set the starting directories
         * <p>
         * Each path will be validated to be a directory that exists
         *
         * @param dirs
         * @return
         * @throws IOException Any path doesnt exist or is not a directory.
         */
        public PathFinder startingInPaths(Collection<Path> dirs) throws IOException{
            this.startingDirs = new ArrayList<>();
            for(Path dir : dirs){
                if(!Files.exists(dir)){
                    throw new IOException(String.format("Directory %s doesnt exist.", dir));
                }
                if(!Files.isDirectory(dir)){
                    throw new IOException(String.format("%s is not a directory.", dir));
                }
                startingDirs.add(dir);
            }
            return this;
        }

        /**
         * Wild card include pattern.
         * <p>
         * @see {@link WildcardFileFilter}
         *
         * @param include  Wildcard filename patterns, e.g. *.md
         * @return
         */
        public PathFinder including(List<String> include){
            this.include = include;
            return this;
        }

        /**
         * Wild card include pattern.
         * <p>
         * @see {@link WildcardFileFilter}
         *
         * @param include Wildcard filename pattern, e.g. *.md
         * @return
         */
        public PathFinder including(String include){
            this.include = Collections.singletonList(include);
            return this;
        }

        /**
         * Wild card include patterns.
         * <p>
         * @see {@link WildcardFileFilter}
         *
         * @param exclude Wildcard filename patterns, e.g. *.md
         * @return
         */
        public PathFinder excluding(List<String> exclude){
            this.exclude = exclude;
            return this;
        }

        /**
         * Wild card include patterns
         * <p>
         * @see {@link WildcardFileFilter}
         *
         * @param exclude Wildcard filename pattern, e.g. *.md
         * @return
         */
        public PathFinder excluding(String exclude){
            this.exclude = Collections.singletonList(exclude);
            return this;
        }

        /**
         * When set, recursively walk directories when finding files. Default:  true.
         *
         * @param isRecursive
         * @return
         */
        public PathFinder recursive(boolean isRecursive){
            this.recursive = isRecursive;
            return this;
        }

        /**
         * Find all files using the builder properties.
         * <p>
         * Files will only be included if they match any of the include patterns and none of the exclude filters.
         *
         * @return List of matching {@link File} or an empty list.
         */
        public Collection<File> files(){
            IOFileFilter dirFilter = recursive ? TrueFileFilter.INSTANCE : null;
            IOFileFilter fileFilter = new AndFileFilter(
                    new WildcardFileFilter(include),
                    new NotFileFilter(new WildcardFileFilter(exclude)));

            return startingDirs.stream()
                    .map(d -> FileUtils.listFiles(d.toFile(), fileFilter, dirFilter))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        /**
         * Same as {@link PathFinder#files()} but files are converted to {@link Path} objects before being returned.
         *
         * @return List of matching {@link Path} or an empty list.
         */
        public Collection<Path> paths(){
            return files().stream()
                    .map(f -> f.toPath())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Save files contents to the file path, overwriting the file if it exists. See {@link Resources#save(String, String, boolean)}
     *
     * @param filePath Path to the file to save. File path cannot be to an existing directory.
     * @param content File contents to write.
     * @throws IOException If the file path cant be overwritten, the file path is invalid or an general IO error occurred.
     */
    public static void save(String filePath, String content) throws IOException {
        Resources.save(filePath, content, true);
    }

    /**
     * Save the string content to a file at the provided file path, optionally overwriting the file if it exists.
     * <p>
     * If the file path exists and is a directory or overwrite is false, an exception will be thrown, otherwise the file
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

    /**
     * Load the contents of the file located at the file path. See {@link FileUtils#readFileToString(File, String)}
     *
     * @param filePath
     * @return File contents as a string.
     * @throws IOException The file doesnt exist or cant be read.
     */
    public static String load(String filePath) throws IOException {
        return FileUtils.readFileToString(Paths.get(filePath).toFile(), (String) null);
    }

    /**
     * Convert the path represented as a string into a {@link Path}.
     *
     * @param rawPath
     * @return
     */
    public static Path path(String rawPath){
        try {
            return path(rawPath, false);
        } catch(FileNotFoundException e){
            throw new RuntimeException("This exception should not have happened, theres a bug in Resources.path(String, boolean)");
        }
    }

    /**
     * Convert the path represented as a string into a {@link Path}, optionally failing if the path doesnt exist.
     *
     * @param rawPath
     * @param failOnNonExistantPath Whether or not to throw an exception if the file deosnt exist.
     * @return
     * @throws FileNotFoundException The path doesnt exist and failOnNonExistantPath is true.
     */
    public static Path path(String rawPath, boolean failOnNonExistantPath) throws FileNotFoundException {
        Path p = Paths.get(rawPath);
        if(failOnNonExistantPath && !Files.exists(p)){
            throw new FileNotFoundException(String.format("File %s doesnt exist", rawPath));
        }
        return p;
    }

    /**
     * Make a collection of {@link Path}s relative the to provided base. See {@link Path#relativize(Path)}
     *
     * @param base Base path other paths should be relative to
     * @param paths
     * @return
     */
    public static Collection<Path> relativeTo(Path base, Collection<Path> paths){
        return paths.stream()
                .map(p -> p.isAbsolute() ? base.relativize(p) : p)
                .collect(Collectors.toList());
    }
}
