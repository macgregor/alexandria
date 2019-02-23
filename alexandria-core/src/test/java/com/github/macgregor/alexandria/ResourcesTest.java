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
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ResourcesTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
