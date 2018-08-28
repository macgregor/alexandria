package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
                .files();
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
    public void testPathFinderWithMultipleInputSources() throws IOException {
        File subDir1 = folder.newFolder("foo");
        File f1 = new File(subDir1, "readme.md");
        f1.createNewFile();
        File subDir2 = folder.newFolder("bar");
        File f2 = new File(subDir2, "readme2.md");
        f2.createNewFile();

        Collection<File> found = new Resources.PathFinder()
                .startingIn(Arrays.asList(subDir1.getPath(), subDir2.getPath()))
                .files();
        assertThat(found.stream().map(File::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "readme2.md");
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
    public void testLoadEmptyFile() throws IOException {
        File f = folder.newFile();
        assertThat(Resources.load(f.getPath())).isNotNull();
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
        Path p = Resources.path("not_there", true);
    }

    @Test
    public void testInterpolationWithNull(){
        assertThat(Resources.interpolate(null)).isNull();
    }

    @Test
    public void testInterpolationWithRegularString(){
        assertThat(Resources.interpolate("foo")).isEqualTo("foo");
    }

    @Test
    public void testInterpolationWithInterpolationString(){
        assertThat(Resources.interpolate("${env.foo}")).isEqualTo(null);
    }

    @Test
    public void testInterpolationWithInterpolationStringWhenEnvSet() throws Exception {
        addEnvVariable("foo", "bar");
        assertThat(Resources.interpolate("${env.foo}")).isEqualTo("bar");
        removeEnvVariable("foo");
    }

    @Test
    public void testInterpolationWithInterpolationStringPrefersEnv() throws Exception {
        System.setProperty("foo", "sys");
        addEnvVariable("foo", "env");
        assertThat(Resources.interpolate("${env.foo}")).isEqualTo("env");
        removeEnvVariable("foo");
    }

    @Test
    public void testInterpolationWithInterpolationStringWhenSystemPropertySet() throws Exception {
        System.setProperty("bar", "baz");
        assertThat(Resources.interpolate("${env.bar}")).isEqualTo("baz");
    }

    public static void addEnvVariable(String key, String value) throws Exception {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for(Class cl : classes) {
            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                Map<String, String> map = (Map<String, String>) obj;
                map.put(key, value);
            }
        }
    }

    public static void removeEnvVariable(String key) throws Exception {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for(Class cl : classes) {
            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                Map<String, String> map = (Map<String, String>) obj;
                map.remove(key);
            }
        }
    }
}
