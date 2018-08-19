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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AlexandriaSyncTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSyncWithJiveRemote() throws BatchProcessException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.JiveRemote");
        config.remote().baseUrl(Optional.of(""));
        config.remote().username(Optional.of(""));
        config.remote().password(Optional.of(""));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Alexandria.syncWithRemote(context);
    }

    @Test
    public void testSyncWithNoopRemote() throws BatchProcessException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Alexandria.syncWithRemote(context);
    }

    @Test
    public void testSyncWithNoopRemoteWithDocument() throws BatchProcessException, IOException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(folder.newFile().toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Alexandria.syncWithRemote(context);
    }

    @Test
    public void testSyncWithNonExistentClass() {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NotAThing");

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        assertThatThrownBy(() -> Alexandria.syncWithRemote(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("com.github.macgregor.alexandria.remotes.NotAThing");
    }

    @Test
    public void testSyncCalculatesChecksum() throws BatchProcessException, IOException {
        File f = folder.newFile();
        Resources.save(f.getPath(), "hello");
        Config config = new Config();
        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(f.toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Alexandria.syncWithRemote(context);
        assertThat(metadata.sourceChecksum()).isPresent();
    }
}
