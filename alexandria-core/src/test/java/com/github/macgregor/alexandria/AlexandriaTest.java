package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AlexandriaTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIndexFresh() throws IOException {
        File f1 = folder.newFile("readme.md");
        File f2 = folder.newFile("doc.md");
        Config config = new Config();
        config.searchPath(Arrays.asList(folder.getRoot().toString()));

        Context context = new Context();
        context.configPath(folder.newFile().toPath());
        context.projectBase(folder.getRoot().toPath());
        context.config(config);

        Alexandria.index(context);
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.sourcePath().toFile().getName())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
    }

    @Test
    public void testIndexUpdate() throws IOException {
        File f1 = folder.newFile("readme.md");
        File f2 = folder.newFile("doc.md");

        Config config = new Config();
        config.searchPath(Arrays.asList(folder.getRoot().toString()));

        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        readmeMetadata.title("Custom Title");
        config.metadata().get().add(readmeMetadata);

        Context context = new Context();
        context.configPath(folder.newFile().toPath());
        context.projectBase(folder.getRoot().toPath());
        context.config(config);

        Alexandria.index(context);
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.sourcePath().toFile().getName())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
        assertThat(config.metadata().get().get(0).title()).isEqualTo("Custom Title");
    }

    @Test
    public void testIndexSetsTitle() throws IOException {
        File f1 = folder.newFile("readme.md");
        File f2 = folder.newFile("doc.md");

        Config config = new Config();
        config.searchPath(Arrays.asList(folder.getRoot().toString()));

        Context context = new Context();
        context.configPath(folder.newFile().toPath());
        context.projectBase(folder.getRoot().toPath());
        context.config(config);

        Alexandria.index(context);
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.title())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
    }

    @Test
    public void testIndexSetsSourcePathRelativeToProjectBase() throws IOException {
        File l1Readme = folder.newFile("readme.md");
        File l2Dir = folder.newFolder("l2");
        File l2Readme = new File(l2Dir, "readme.md");
        l2Readme.createNewFile();
        File l3Dir = new File(l2Dir, "l3");
        l3Dir.mkdir();
        File l3Readme = new File(l3Dir, "readme.md");
        l3Readme.createNewFile();

        Config config = new Config();
        config.searchPath(Arrays.asList(folder.getRoot().toString()));
        Context context = new Context();
        context.configPath(folder.newFile().toPath());
        context.projectBase(Paths.get(folder.getRoot().toString()));
        context.config(config);

        Alexandria.index(context);
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()).hasSize(3);

        Path[] expected = new Path[3];
        expected[0] = Paths.get(folder.getRoot().toString()).relativize(l1Readme.toPath());
        expected[1] = Paths.get(folder.getRoot().toString()).relativize(l2Readme.toPath());
        expected[2] = Paths.get(folder.getRoot().toString()).relativize(l3Readme.toPath());
        assertThat(config.metadata().get().stream()
                .map(m -> m.sourcePath())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testConvertDoesntConvertWhenRemoteSupportsMarkdown() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.supportsNativeMarkdown(Optional.of(true));
        config.remote(remoteConfig);

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria.convert(context);
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).doesNotExist();
    }

    @Test
    public void testConvertSetsConvertedPathPreferringConfigOverride() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");
        File subdir = folder.newFolder("out");

        Config config = new Config();
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);
        config.output(Optional.of(subdir.getPath()));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria.convert(context);
        assertThat(context.convertedPath(readmeMetadata).get()).isEqualTo(Paths.get(subdir.getPath(), "readme.html"));
        assertThat(Paths.get(subdir.getPath(), "readme.html")).exists();
    }

    @Test
    public void testConvertSetsConvertedPathUsesSourceDirAsOutput() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria.convert(context);
        assertThat(context.convertedPath(readmeMetadata).get()).isEqualTo(Paths.get(folder.getRoot().toString(), "readme.html"));
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).exists();
    }
}
