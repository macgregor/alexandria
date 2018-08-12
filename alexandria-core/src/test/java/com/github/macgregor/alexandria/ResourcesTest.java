package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ResourcesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testPathFinderFindsAllFilesByDefault() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
               .containsExactlyInAnyOrder("readme.md", "readme2.md", "foo.txt");
    }

    @Test
    public void testPathFinderFindsFilesMatchingIncludePattern() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .including("*.md")
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testPathFinderFindsFilesMatchingAllIncludePatterns() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        folder.newFile("MyClass.java");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .including(Arrays.asList("*.md", "*.txt"))
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md", "foo.txt");
    }

    @Test
    public void testPathFinderFindsFilesNotMatchingExcludePattern() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .excluding("*.txt")
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testPathFinderFindsFilesNotMatchingAnyExcludePatterns() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        folder.newFile("MyClass.java");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .excluding(Arrays.asList("*.txt", "*.java"))
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testPathFinderOnlyMatchesFiles() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFolder("foo");
        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testPathFinderFindsAllFilesRecursivelyByDefault() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();

        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md", "readme3.md");
    }

    @Test
    public void testPathFinderFindsAllFilesObeysRecursionFlag() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();

        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .recursive(false)
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testPathFinderMatchesOnIncludeAndExcludePatterns() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme.txt");

        Collection<File> found = new Resources.PathFinder()
                .startingIn(folder.getRoot().toString())
                .including("*")
                .excluding("*.txt")
                .find();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md");
    }

    @Test
    public void testPathFinderChecksStartingDirExists() throws IOException {
        Resources.PathFinder pathFinder = new Resources.PathFinder();
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> pathFinder.startingIn("nope"))
                .withMessageContaining("Directory nope doesnt exist.");
    }

    @Test
    public void testPathFinderChecksStartingDirIsDirectory() throws IOException {
        File file = folder.newFile();
        Resources.PathFinder pathFinder = new Resources.PathFinder();
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> pathFinder.startingIn(file.toString()))
                .withMessageContaining("is not a directory.");
    }

    @Test
    public void testSaveNewFile() throws IOException {
        File f = new File(folder.getRoot(), "out");
        Resources.save(f.getPath(), "hello");
        assertThat(f).hasContent("hello");
    }

    @Test
    public void testSaveOverwritesFile() throws IOException {
        File f = new File(folder.getRoot(), "out");
        Resources.save(f.getPath(), "hello");
        Resources.save(f.getPath(), "world");
        assertThat(f).hasContent("world");
    }

    @Test
    public void testSaveRefusesToOverwriteFile() throws IOException {
        File f = new File(folder.getRoot(), "out");
        Resources.save(f.getPath(), "hello");
        ;
        assertThatExceptionOfType(FileAlreadyExistsException.class)
                .isThrownBy(() -> Resources.save(f.getPath(), "world", false))
                .withMessageContaining("Refusing to overwrite existing file.");
    }

    @Test
    public void testSaveRefusesToOverwriteDirectory() throws IOException {
        File f = folder.newFolder("subdir");
        assertThatExceptionOfType(FileAlreadyExistsException.class)
                .isThrownBy(() -> Resources.save(f.getPath(), "world"))
                .withMessageContaining("File is a directory. Refusing to destroy.");
    }

    @Test
    public void testPathsConvertsAllStrings() throws FileNotFoundException {
        String path1 = "foo.txt";
        String path2 = "dir/bar.txt";
        Set<Path> paths = Resources.paths(Arrays.asList(path1, path2));
        assertThat(paths.stream().map(Path::toString).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("foo.txt", "dir/bar.txt");
    }

    @Test
    public void testPathsChecksForExistence() throws IOException {
        File f1 = folder.newFile("readme.md");
        File subDir = folder.newFolder("foo");
        File f2 = new File(subDir, "readme.md");
        f2.createNewFile();

        Set<Path> paths = Resources.paths(Arrays.asList(f1.toString(), f2.toString()), true);
        assertThat(paths).containsExactlyInAnyOrder(f1.toPath(), f2.toPath());
    }

    @Test(expected = FileNotFoundException.class)
    public void testPathsChecksForExistenceFails() throws IOException {
        Resources.paths(Arrays.asList("nope"), true);
    }

    @Test
    public void testPathsIgnoresDirectories() throws IOException {
        File f1 = folder.newFile("readme.md");
        File subDir = folder.newFolder("foo");
        File f2 = new File(subDir, "readme.md");
        f2.createNewFile();

        Set<Path> paths = Resources.paths(Arrays.asList(f1.toString(), subDir.toString(), f2.toString()), false, false, true);
        assertThat(paths).containsExactlyInAnyOrder(f1.toPath(), f2.toPath());
    }

    @Test
    public void testFilePathsIgnoresDirectories() throws IOException {
        File f1 = folder.newFile("readme.md");
        File subDir = folder.newFolder("foo");
        File f2 = new File(subDir, "readme.md");
        f2.createNewFile();

        Set<Path> paths = Resources.filePaths(Arrays.asList(f1.toString(), subDir.toString(), f2.toString()), false);
        assertThat(paths).containsExactlyInAnyOrder(f1.toPath(), f2.toPath());
    }

    @Test
    public void testPathsIgnoresFiles() throws IOException {
        File f1 = folder.newFile("readme.md");
        File subDir = folder.newFolder("foo");
        File f2 = new File(subDir, "readme.md");
        f2.createNewFile();

        Set<Path> paths = Resources.paths(Arrays.asList(f1.toString(), subDir.toString(), f2.toString()), false, true, false);
        assertThat(paths).containsExactlyInAnyOrder(subDir.toPath());
    }

    @Test
    public void testDirectoryPathsIgnoresFiles() throws IOException {
        File f1 = folder.newFile("readme.md");
        File subDir = folder.newFolder("foo");
        File f2 = new File(subDir, "readme.md");
        f2.createNewFile();

        Set<Path> paths = Resources.directoryPaths(Arrays.asList(f1.toString(), subDir.toString(), f2.toString()), false);
        assertThat(paths).containsExactlyInAnyOrder(subDir.toPath());
    }

    @Test
    public void testPathConvertsString(){
        Path p = Resources.path("readme.md");
        assertThat(p).isEqualTo(Paths.get("readme.md"));
    }

    @Test
    public void testPathDoesntCheckForExistence() throws FileNotFoundException {
        Path p = Resources.path("readme.md", false);
        assertThat(p).isEqualTo(Paths.get("readme.md"));
    }

    @Test(expected = FileNotFoundException.class)
    public void testPathChecksForExistence() throws FileNotFoundException {
        Path p = Resources.path("readme.md", true);
    }
}
