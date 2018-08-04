package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourcesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testFilesFindsAllFilesByDefault() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        List<File> found = Resources.files(folder.getRoot().getPath());
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
               .containsExactlyInAnyOrder("readme.md", "readme2.md", "foo.txt");
    }

    @Test
    public void testFilesFindsAllFilesByRegex() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("foo.txt");
        List<File> found = Resources.files(folder.getRoot().getPath(), "regex:.*");
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md", "foo.txt");
    }

    @Test
    public void testFilesOnlyMatchesFiles() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFolder("foo");

        List<File> found = Resources.files(folder.getRoot().getPath());
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testFilesFindsAllFilesRecursivelyByDefault() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();

        List<File> found = Resources.files(folder.getRoot().getPath(), "regex:.*");
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md", "readme3.md");
    }

    @Test
    public void testFilesFindsAllFilesRecursivelyByRegex() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();

        List<File> found = Resources.files(folder.getRoot().getPath());
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md", "readme3.md");
    }

    @Test
    public void testFilesOnlyMatchesFilesByPattern() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        folder.newFile("ignore");
        List<File> found = Resources.files(folder.getRoot().getPath(), "glob:**.md");
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test(expected = FileNotFoundException.class)
    public void testFilesThrowsFileNotFound() throws IOException {
        Resources.files("nope");
    }

    @Test
    public void testFilesObeysMaxDepth() throws IOException {
        folder.newFile("readme.md");
        folder.newFile("readme2.md");
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();

        List<File> found = Resources.files(folder.getRoot().getPath(), 1);
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
    }

    @Test
    public void testFilesWithDirectoryAsFilePath() throws IOException {
        folder.newFile("readme.md");
        List<File> found = Resources.files(folder.getRoot().getPath());
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactly("readme.md");
    }

    @Test
    public void testFilesWithFileAsFilePath() throws IOException {
        File f = folder.newFile("readme.md");
        List<File> found = Resources.files(f.getPath());
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactly("readme.md");
    }

    @Test
    public void testFilesWithFileAsFilePathMatchesPattern() throws IOException {
        File f = folder.newFile("readme.md");
        List<File> found = Resources.files(f.getPath(), "glob:**.md");
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactly("readme.md");
    }

    @Test
    public void testFilesWithFileAsFilePathDoesntMatchesPattern() throws IOException {
        File f = folder.newFile("readme.txt");
        List<File> found = Resources.files(f.getPath(), "glob:**.md");
        assertThat(found).isEmpty();
    }

    @Test
    public void testFilesWithSymbolicLinkToFile() throws IOException {
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "readme3.md");
        f3.createNewFile();
        Path link = Paths.get(folder.getRoot().getPath(), "link");
        Files.createSymbolicLink(link, Paths.get(f3.toURI()));

        List<File> found = Resources.files(folder.getRoot().getPath(), 1);
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("link");
    }

    @Test
    public void testFilesWithSymbolicLinkToFileMatchesPattern() throws IOException {
        File subDir = folder.newFolder("foo");
        File f3 = new File(subDir, "link");
        f3.createNewFile();
        Path link = Paths.get(folder.getRoot().getPath(), "readme.md");
        Files.createSymbolicLink(link, Paths.get(f3.toURI()));

        List<File> found = Resources.files(folder.getRoot().getPath(), "glob:**.md", 1);
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md");
    }

    @Test
    public void testFilesWithSymbolicLinkToDir() throws IOException {
        File subDirL1 = folder.newFolder("level1");
        File subDirL2 = new File(subDirL1, "level2");
        subDirL2.mkdir();
        File file = new File(subDirL2, "readme.md");
        file.createNewFile();
        Path link = Paths.get(folder.getRoot().getPath(), "link");
        Files.createSymbolicLink(link, Paths.get(subDirL2.toURI()));

        List<File> found = Resources.files(link.toString(), 1);
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md");
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

    @Test(expected = FileAlreadyExistsException.class)
    public void testSaveRefusesToOverwriteFile() throws IOException {
        File f = new File(folder.getRoot(), "out");
        Resources.save(f.getPath(), "hello");
        Resources.save(f.getPath(), "world", false);
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
