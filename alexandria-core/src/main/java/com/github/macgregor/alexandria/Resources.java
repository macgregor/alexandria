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

/**
 * Utility class for working with file system resources.
 */
public class Resources {

    public static class PathFinder{

        private Path startingDir;
        private List<String> include;
        private List<String> exclude;
        private boolean recursive;

        public PathFinder(){
            include = Collections.singletonList("*");
            exclude = Collections.emptyList();
            recursive = true;
        }

        public PathFinder startingIn(String dir) throws IOException {
            this.startingDir = Paths.get(dir);
            if(!Files.exists(startingDir)){
                throw new IOException(String.format("Directory %s doesnt exist.", dir));
            }
            if(!Files.isDirectory(startingDir)){
                throw new IOException(String.format("%s is not a directory.", dir));
            }
            return this;
        }

        public PathFinder including(List<String> include){
            this.include = include;
            return this;
        }

        public PathFinder including(String include){
            this.include = Collections.singletonList(include);
            return this;
        }

        public PathFinder excluding(List<String> exclude){
            this.exclude = exclude;
            return this;
        }

        public PathFinder excluding(String exclude){
            this.exclude = Collections.singletonList(exclude);
            return this;
        }

        public PathFinder recursive(boolean isRecursive){
            this.recursive = isRecursive;
            return this;
        }

        public Collection<File> find(){
            IOFileFilter dirFilter = recursive ? TrueFileFilter.INSTANCE : null;
            IOFileFilter fileFilter = new AndFileFilter(
                    new WildcardFileFilter(include),
                    new NotFileFilter(new WildcardFileFilter(exclude)));

            return FileUtils.listFiles(startingDir.toFile(), fileFilter, dirFilter);
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
