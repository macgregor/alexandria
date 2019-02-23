package com.github.macgregor.alexandria;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for working with file system resources.
 *
 * Find files/paths/directories that match patterns, saving and loading files, etc.
 */
public class Resources {
    public static final String VARIABLE_INTERPOLATION_PATTERN = "\\$\\{env\\.(.*)\\}";

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
     *
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
     * @param filePath  file path to load
     * @return File  contents as a string.
     * @throws IOException  The file doesnt exist or cant be read.
     */
    public static String load(String filePath) throws IOException {
        return FileUtils.readFileToString(Paths.get(filePath).toFile(), (String) null);
    }

    public static boolean fileContentsAreBlank(String filePath) throws IOException {
        return StringUtils.isBlank(load(filePath));
    }

    /**
     * Convert the path represented as a string into a {@link Path}.
     *
     * @param rawPath  string path to convert
     * @return  Path representation of the string
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
     * @param rawPath  string path to convert
     * @param failOnNonExistantPath  Whether or not to throw an exception if the file deosnt exist.
     * @return  Path representation of the string
     * @throws FileNotFoundException  The path doesnt exist and failOnNonExistantPath is true.
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
     * @param base  Base path other paths should be relative to
     * @param paths  paths to make relative
     * @return  new collection of relative paths
     */
    public static Collection<Path> relativeTo(Path base, Collection<Path> paths){
        if(paths == null){
            return null;
        }
        return paths.stream()
                .map(p -> p.isAbsolute() ? base.relativize(p) : p)
                .collect(Collectors.toList());
    }

    /**
     * Make a {@link Path} relative to the provided base. See {@link Path#relativize(Path)}.
     *
     * @param base  Base path other paths should be relative to
     * @param path  path to make relative
     * @return  if path is null, null; otherwise a relative path
     */
    public static Path relativeTo(Path base, Path path){
        if(path == null){
            return null;
        }
        return base.relativize(path);
    }

    /**
     * Make a collection of relative paths absolute using the provided base as a reference point.
     *
     * @param base  absolute base path to resolve the relative path against
     * @param paths  the relative path to resolve
     * @return  if paths is null, null; otherwise the absolute representation of the paths
     */
    public static Collection<Path> absolutePath(Path base, Collection<Path> paths){
        if(paths == null){
            return null;
        }
        List<Path> converted = new ArrayList<>(paths.size());
        for(Path p : paths){
            converted.add(absolutePath(base, p));
        }
        return converted;
    }

    /**
     * Make a relative path absolute using the provided base as a reference point.
     *
     * @param base  absolute base path to resolve the relative path against
     * @param path  the relative path to resolve
     * @return  if path is null; null otherwise the absolute representation of path
     */
    public static Path absolutePath(Path base, Path path){
        if(path == null || path.isAbsolute()){
            return path;
        }
        return base.resolve(path).normalize();
    }

    /**
     * Interpolate a variable string (e.g. ${env.foo}) resolving to an environment variable or system property.
     *
     * The interpolation pattern is {@value VARIABLE_INTERPOLATION_PATTERN}. So ${env.foo} will will look up
     * an envrionmental or system variable called foo. If the input string doesnt match the pattern, the input string is
     * returned. Otherwise the extracted variable name is passed to {@link System#getenv(String)} which returns
     * null if the variable isnt set.
     *
     * @param input  the input string to interpolate
     * @return  The input string if it isnt an interpolation pattern, otherwise the environment/system variable or null if it isnt set
     */
    public static String interpolate(String input){
        if(input != null) {
            Pattern p = Pattern.compile(VARIABLE_INTERPOLATION_PATTERN);
            Matcher m = p.matcher(input);
            if (m.matches()) {
                if(System.getenv(m.group(1)) != null){
                    return System.getenv(m.group(1));
                }
                if(System.getProperty(m.group(1)) != null){
                    return System.getProperty(m.group(1));
                }
                return null;
            }
        }
        return input;
    }
}
