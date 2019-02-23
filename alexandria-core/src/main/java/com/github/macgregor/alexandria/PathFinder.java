package com.github.macgregor.alexandria;

import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Find files along a set of directories that based on include/exclude patterns.
 *
 * Makes use of {@link FileUtils#listFiles(File, IOFileFilter, IOFileFilter)} to do most of the work.
 */
public class PathFinder{

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
     *
     * Path will be validated to be a directory that exists
     *
     * @param dir  dir to use
     * @return  builder
     * @throws IOException  The path doesnt exist or is not a directory.
     */
    public PathFinder startingIn(String dir) throws IOException {
        return startingIn(Collections.singletonList(dir));
    }

    /**
     * Set the starting directories, converting the provided string paths into {@link Path} objects.
     *
     * Each path will be validated to be a directory that exists
     *
     * @param dirs  dirs to use
     * @return  builder
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
     *
     * Path will be validated to be a directory that exists
     *
     * @param dir  dir to use
     * @return  builder
     * @throws IOException  The path doesnt exist or is not a directory.
     */
    public PathFinder startingInPath(Path dir) throws IOException {
        return startingInPaths(Collections.singletonList(dir));
    }

    /**
     * Set the starting directories
     *
     * Each path will be validated to be a directory that exists
     *
     * @param dirs  dirs to use
     * @return  builder
     * @throws IOException  Any path doesnt exist or is not a directory.
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
     * Patterns to match file paths against for inclusion.
     *
     * @see WildcardFileFilter
     * @see GlobFileFilter
     * @see RelativeFileFilter
     *
     * @param include  Path patterns to include, e.g. *.md, ./foo.txt, **\/docs\/**
     * @return  builder
     */
    public PathFinder including(List<String> include){
        this.include = include;
        return this;
    }

    /**
     * Patterns to match file paths against for inclusion.
     *
     * @see WildcardFileFilter
     * @see GlobFileFilter
     * @see RelativeFileFilter
     *
     * @param include  Path patterns to include, e.g. *.md, ./foo.txt, **\/docs\/**
     * @return  builder
     */
    public PathFinder including(String include){
        this.include = Collections.singletonList(include);
        return this;
    }

    /**
     *  Patterns to match file paths against for exclusion.
     *
     * @see WildcardFileFilter
     * @see GlobFileFilter
     * @see RelativeFileFilter
     *
     * @param exclude  Path patterns to exclude, e.g. *.md, ./foo.txt, **\/docs\/**
     * @return  builder
     */
    public PathFinder excluding(List<String> exclude){
        this.exclude = exclude;
        return this;
    }

    /**
     * Patterns to match file paths against for exclusion.
     *
     * @see WildcardFileFilter
     * @see GlobFileFilter
     * @see RelativeFileFilter
     *
     * @param exclude  Path patterns to exclude, e.g. *.md, ./foo.txt, **\/docs\/**
     * @return  builder
     */
    public PathFinder excluding(String exclude){
        this.exclude = Collections.singletonList(exclude);
        return this;
    }

    /**
     * When set, recursively walk directories when finding files. Default:  true.
     *
     * @param isRecursive  whether to use recursion or not
     * @return  builder
     */
    public PathFinder recursive(boolean isRecursive){
        this.recursive = isRecursive;
        return this;
    }

    /**
     * Find all files using the builder properties.
     *
     * Files will only be included if they match any of the include patterns and none of the exclude filters.
     *
     * Patterns can be wildcards against any file names (see {@link WildcardFileFilter}, specified relative to any
     * {@link PathFinder#startingDirs} path (see {@link RelativeFileFilter}) or a glob pattern (see {@link GlobFileFilter}).
     *
     * @return  List of matching {@link File} or an empty list.
     */
    public Collection<File> files(){
        IOFileFilter dirFilter = recursive ? TrueFileFilter.INSTANCE : null;
        IOFileFilter includeFilter = FileFilterUtils.or(
                new WildcardFileFilter(include),
                new GlobFileFilter(include),
                new RelativeFileFilter((List<Path>) startingDirs, include));
        IOFileFilter excludeFilter = new NotFileFilter(FileFilterUtils.or(
                    new WildcardFileFilter(exclude),
                    new GlobFileFilter(exclude),
                    new RelativeFileFilter((List<Path>) startingDirs, exclude)));
        IOFileFilter fileFilter = new AndFileFilter(includeFilter, excludeFilter);

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

    /**
     * File filter that matches glob patterns against absolute file paths.
     *
     * For example, if you have a directory structure like this:
     * <pre>
     * home
     * └── project
     *     ├── exclude
     *     │   └── README.md
     *     └── include
     *         ├── README.md
     *         └── subdir
     *             └── foo.md
     * </pre>
     * where {@code /home/project} is the base directory being scanned, then {@code /home/project/include/*.md} would match
     * {@code /home/project/include/README.md} but not {@code /home/project/exclude/README.md} or
     * {@code /home/project/include/subdir/foo.md}. {@code **include/**} would match both {@code .md} files under the
     * {@code include} directory, which feels a bit unnatural and could lead to inadvertent matches as directories grow.
     * {@link RelativeFileFilter} adds this ability.
     *
     * For full details on glob syntax see {@link FileSystem#getPathMatcher(String)}.
     */
    public static class GlobFileFilter extends AbstractFileFilter{

        @NonNull protected List<String> wildcards;

        /**
         *
         * @param wildcard wildcard glob pattern
         * @throws IllegalArgumentException if {@param wildcard} is null
         */
        public GlobFileFilter(String wildcard) {
            if (wildcard == null) {
                throw new IllegalArgumentException("The wildcard must not be null");
            }
            this.wildcards = Arrays.asList(glob(wildcard));
        }

        /**
         *
         * @param wildcards wildcard glob patterns
         * @throws IllegalArgumentException if {@param wildcards} is null
         */
        public GlobFileFilter(List<String> wildcards) {
            if (wildcards == null) {
                throw new IllegalArgumentException("The wildcard must not be null");
            }
            this.wildcards = glob(wildcards);
        }

        /**
         * Checks to see if the filename matches one of the glob patterns.
         *
         * @param dir parent dir containing the file being checked
         * @param name file name itself
         * @return true if the file path matches one of the patterns
         */
        @Override
        public boolean accept(final File dir, final String name) {
            Path path = Paths.get(dir.getAbsolutePath(), name);
            return matches(path);
        }

        /**
         * Checks to see if the filename matches one of the glob patterns.
         *
         * @param file  the file to check
         * @return true if the file path matches one of the pattern
         */
        @Override
        public boolean accept(final File file) {
            Path path = file.toPath().toAbsolutePath();
            return matches(path);
        }

        /**
         * Logic behind {@link GlobFileFilter#accept(File)} and {@link GlobFileFilter#accept(File, String)}
         *
         * @param path absolute file path to check
         * @return true if the file path matches one of the pattern
         */
        protected boolean matches(Path path){
            PathMatcher pathMatcher = null;
            for (final String wildcard : wildcards) {
                pathMatcher = FileSystems.getDefault().getPathMatcher(wildcard);
                if(pathMatcher.matches(path)){
                    return true;
                }
            }
            return false;
        }

        /**
         * Adds the {@code glob:} prefix to the pattern if it doesnt already have it.
         *
         * @param globPattern glob pattern string to prefix
         * @return prefixed glob pattern
         */
        protected String glob(String globPattern){
            return globPattern.startsWith("glob:") ? globPattern : "glob:"+globPattern;
        }

        /**
         * Same as {@link GlobFileFilter#glob(String)} for a list of patterns.
         *
         * @param globPatterns glob pattern strings to prefix
         * @return prefixed glob patterns
         */
        protected List<String> glob(List<String> globPatterns){
            List<String> globs = new ArrayList<>(globPatterns.size());

            for(int i =0; i < globPatterns.size(); i++){
                globs.add(i, glob(globPatterns.get(i)));
            }
            return globs;
        }
    }

    /**
     * File filter that matches glob patterns relative to a set of base paths. This builds on {@link GlobFileFilter} and
     * lets people reference relative paths which most intuitively expect to work.
     *
     * For example, if you have a directory structure like this:
     * <pre>
     * home
     * └── project
     *     ├── exclude
     *     │   └── README.md
     *     └── include
     *         ├── README.md
     *         └── subdir
     *             └── foo.md
     * </pre>
     * where {@code /home/project} is the base directory being scanned, then {@code ./include/*.md} would match
     * {@code /home/project/include/README.md} but not {@code /home/project/exclude/README.md} or
     * {@code /home/project/include/subdir/foo.md}. {@code ./include/**} would match both {@code .md} files under the
     * {@code include} directory, without being relative path aware this would need to be rewritten to {@code **include/**}
     * which doesnt feel as natural.
     *
     * For full details on glob syntax see {@link FileSystem#getPathMatcher(String)}.
     */
    public static class RelativeFileFilter extends GlobFileFilter {
        private List<Path> basePaths;

        /**
         *
         * @param basePath base path files will be made relative to before matching. {@param basePath} will be made absolute
         *                 before being used.
         * @param wildcard wildcard glob pattern
         * @throws IllegalArgumentException if {@param basePath} or {@param wildcard} are null
         */
        public RelativeFileFilter(Path basePath, String wildcard){
            super(wildcard);
            if (basePath == null) {
                throw new IllegalArgumentException("Relative base paths must not be null");
            }
            this.basePaths = Arrays.asList(basePath);
        }

        /**
         *
         * @param basePath base path files will be made relative to before matching. {@param basePath} will be made absolute
         *                 before being used.
         * @param wildcards wildcard glob patterns
         * @throws IllegalArgumentException if {@param basePath} or {@param wildcards} are null
         */
        public RelativeFileFilter(Path basePath, List<String> wildcards){
            super(wildcards);
            if (basePath == null) {
                throw new IllegalArgumentException("Relative base paths must not be null");
            }
            this.basePaths = Arrays.asList(basePath);
        }

        /**
         *
         * @param basePaths base paths files will be made relative to before matching. {@param basePaths} will be made absolute
         *                 before being used.
         * @param wildcard wildcard glob pattern
         * @throws IllegalArgumentException if {@param basePath} or {@param wildcard} are null
         */
        public RelativeFileFilter(List<Path> basePaths, String wildcard){
            super(wildcard);
            if (basePaths == null) {
                throw new IllegalArgumentException("Relative base paths must not be null");
            }
            this.basePaths = basePaths;
        }

        /**
         *
         * @param basePaths base paths files will be made relative to before matching. {@param basePaths} will be made absolute
         *                 before being used.
         * @param wildcards wildcard glob patterns
         * @throws IllegalArgumentException if {@param basePath} or {@param wildcards} are null
         */
        public RelativeFileFilter(List<Path> basePaths, List<String> wildcards){
            super(wildcards);
            if (basePaths == null) {
                throw new IllegalArgumentException("Relative base paths must not be null");
            }
            this.basePaths = basePaths;
        }


        /**
         * Checks to see if the filename matches one of the glob patterns, first ensuring the pattern is properly made
         * relative to the base path it is being compared against.
         *
         * @param dir parent dir containing the file being checked
         * @param name file name itself
         * @return true if the file path matches one of the patterns
         */
        @Override
        public boolean accept(final File dir, final String name) {
            return accept(Paths.get(dir.getAbsolutePath(), name).toFile());
        }

        /**
         * Checks to see if the filename matches one of the glob patterns, first ensuring the pattern is properly made
         * relative to the base path it is being compared against.
         *
         * @param file  the file to check
         * @return true if the file path matches one of the pattern
         */
        @Override
        public boolean accept(final File file) {
            Path path = null;
            for (int i = 0; i < basePaths.size(); i++){
                path = Paths.get("./", Resources.relativeTo(basePaths.get(i).toAbsolutePath(), file.toPath()).toString());
                if(matches(path)){
                    return true;
                }
            }
            return false;
        }
    }
}
