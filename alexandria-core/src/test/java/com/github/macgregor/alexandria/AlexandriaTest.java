package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AlexandriaTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIndexFresh() throws IOException {
        File f1 = folder.newFile("readme.md");
        File f2 = folder.newFile("doc.md");
        AlexandriaConfig config = new AlexandriaConfig();
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

        AlexandriaConfig config = new AlexandriaConfig();
        config.searchPath(Arrays.asList(folder.getRoot().toString()));
        config.configPath(folder.newFile().toPath());

        AlexandriaConfig.DocumentMetadata readmeMetadata = new AlexandriaConfig.DocumentMetadata();
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

        AlexandriaConfig config = new AlexandriaConfig();
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
}
