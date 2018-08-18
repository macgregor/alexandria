package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
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
        config.configPath(folder.newFile().toPath());
        Alexandria.index(config);
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
        config.configPath(folder.newFile().toPath());

        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        readmeMetadata.title("Custom Title");
        config.metadata().get().add(readmeMetadata);

        Alexandria.index(config);
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
        config.configPath(folder.newFile().toPath());

        Alexandria.index(config);
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.title())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
    }

    @Test
    public void testConvertDoesntConvertWhenRemoteSupportsMarkdown() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        config.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.supportsNativeMarkdown(Optional.of(true));
        config.remote(remoteConfig);

        Alexandria.convert(config);
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).doesNotExist();
    }

    @Test
    public void testConvertSetsConvertedPathPreferringConfigOverride() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");
        File subdir = folder.newFolder("out");

        Config config = new Config();
        config.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);
        config.output(Optional.of(subdir.getPath()));

        Alexandria.convert(config);
        assertThat(readmeMetadata.convertedPath().get()).isEqualTo(Paths.get(subdir.getPath(), "readme.html"));
        assertThat(Paths.get(subdir.getPath(), "readme.html")).exists();
    }

    @Test
    public void testConvertSetsConvertedPathUsesSourceDirAsOutput() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        config.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);

        Alexandria.convert(config);
        assertThat(readmeMetadata.convertedPath().get()).isEqualTo(Paths.get(folder.getRoot().toString(), "readme.html"));
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).exists();
    }
}
