package com.github.macgregor.alexandria;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for working with file system resources.
 */
public class Resources {

    public static final String DEFAULT_MATCH_PATTERN = "glob:**";
    public static final String MATCH_MD_FILES = "glob:**.md";

    public static List<File> files(String filePath) throws IOException {
        return Resources.files(filePath, DEFAULT_MATCH_PATTERN);
    }

    public static List<File> files(Path filePath) throws IOException {
        return Resources.files(filePath, DEFAULT_MATCH_PATTERN);
    }

    public static List<File> files(String filePath, String matchPattern) throws IOException {
        return Resources.files(filePath, matchPattern, Integer.MAX_VALUE);
    }

    public static List<File> files(Path filePath, String matchPattern) throws IOException {
        return Resources.files(filePath, matchPattern, Integer.MAX_VALUE);
    }

    public static List<File> files(String filePath, int maxDepth) throws IOException {
        return Resources.files(filePath, DEFAULT_MATCH_PATTERN, maxDepth);
    }

    public static List<File> files(Path filePath, int maxDepth) throws IOException {
        return Resources.files(filePath, DEFAULT_MATCH_PATTERN, maxDepth);
    }

    public static List<File> files(String filePath, String matchPattern, int maxDepth) throws IOException {
        Path start = Paths.get(filePath);
        return files(start, matchPattern, maxDepth);
    }

    /**
     * Retrieve all files on the file path that match pattern with limited recursion depth. The path will be searched
     * recursively, but only files will be returned, even if the directory name matches the match pattern. Symbolic and
     * hard links will be followed, the pattern matcher applies to the name of the link, not the resolved name
     * of the link.
     *
     * Pattern matching is done using {@link java.nio.file.FileSystem#getPathMatcher(String)} and can use "glob"
     * and "regex" pattern matchers (possibly more depending on the file system). For example the default pattern
     * to match all files would be "glob:**" or "regex:.*". To match all markdown files you would use "glob:**.md"
     * or "regex:.*\.md". See {@link java.nio.file.FileSystem#getPathMatcher(String)} for more details and examples.
     *
     * If the file path doesnt exist, is an invalid path, or the match pattern is invalid, an exception is thrown.
     *
     * @param filePath Path to file or directory to search. If a path to a file is provided, that file will be returned
     *                 provided it matches the matchPattern. If it is a directory, it will be walked to find all files that
     *                 match the pattern.
     * @param matchPattern File name pattern to match. See {@link java.nio.file.FileSystem#getPathMatcher(String)}.
     * @param maxDepth Maximum recursion depth for the file walker. See {@link Files#walk(Path, int, FileVisitOption...)}.
     * @return
     * @throws IOException If the file path doesnt exist, is an invalid path, or the match pattern is invalid, an exception is thrown.
     */
    public static List<File> files(Path filePath, String matchPattern, int maxDepth) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(matchPattern);

        if(!Files.exists(filePath)){
            throw new FileNotFoundException(String.format("%s doesnt exist.", filePath));
        }

        try (Stream<Path> paths = Files.walk(filePath, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
            return paths.filter(Files::isRegularFile)
                    .filter(matcher::matches)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    public static void save(String filePath, String content) throws IOException {
        Resources.save(filePath, content, true);
    }

    public static void save(Path filePath, String content) throws IOException {
        Resources.save(filePath, content, true);
    }

    public static void save(String filePath, String content, boolean overwrite) throws IOException {
        Path path = Paths.get(filePath);
        save(path, content, overwrite);
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
    public static void save(Path filePath, String content, boolean overwrite) throws IOException {
        if(Files.exists(filePath)){
            if(Files.isDirectory(filePath)){
                throw new FileAlreadyExistsException("File is a directory. Refusing to destroy.");
            } else if(!overwrite) {
                throw new FileAlreadyExistsException("Refusing to overwrite existing file.");
            } else{
                Files.delete(filePath);
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()));
        writer.write(content);
        writer.close();
    }

    public static String load(String filePath) throws IOException {
        return load(Paths.get(filePath));
    }

    public static String load(Path filePath) throws IOException {
        byte[] encoded = Files.readAllBytes(filePath);
        return new String(encoded, Charset.defaultCharset());
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
