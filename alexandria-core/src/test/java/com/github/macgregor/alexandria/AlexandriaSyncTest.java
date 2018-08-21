package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
    }

    @Test
    public void testSyncWithNoopRemote() throws BatchProcessException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
    }

    @Test
    public void testSyncWithNonExistentClass() {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NotAThing");

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        assertThatThrownBy(() -> alexandria.syncWithRemote())
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

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        assertThat(metadata.sourceChecksum()).isPresent();
    }

    @Test
    public void testSyncCreatesDocument() throws BatchProcessException, IOException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(folder.newFile().toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
    }

    @Test
    public void testSyncUpdatesDocumentNoChecksum() throws BatchProcessException, IOException, URISyntaxException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(folder.newFile().toPath());
        metadata.remoteUri(Optional.of(new URI("http://www.google.com")));
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();

        //todo: need a way to confirm update was actually called
    }

    @Test
    public void testSyncUpdatesDocumentWithSameChecksum() throws BatchProcessException, IOException, URISyntaxException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(folder.newFile().toPath());
        metadata.remoteUri(Optional.of(new URI("http://www.google.com")));
        metadata.sourceChecksum(Optional.of(FileUtils.checksumCRC32(metadata.sourcePath().toFile())));
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        //todo: need a way to confirm update was not called
    }

    @Test
    public void testSyncUpdatesDocumentWithDifferentChecksum() throws BatchProcessException, IOException, URISyntaxException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(folder.newFile().toPath());
        metadata.remoteUri(Optional.of(new URI("http://www.google.com")));
        metadata.sourceChecksum(Optional.of(1234L));
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        //todo: need a way to confirm update was not called
    }

    @Test
    public void testSyncRecreatesConvertedPathData() throws BatchProcessException, IOException {
        File f1 = folder.newFile("hello.md");
        File f2 = folder.newFile("hello.html");
        Resources.save(f1.getPath(), "hello");
        Resources.save(f2.getPath(), "hello");

        Config config = new Config();
        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(f1.toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        assertThat(context.convertedPath(metadata).get()).isEqualTo(f2.toPath());
    }

    @Test
    public void testSyncReconvertsHtml() throws BatchProcessException, IOException {
        File f1 = folder.newFile("hello.md");
        File f2 = folder.newFile("hello.html");
        Resources.save(f1.getPath(), "hello");

        Config config = new Config();
        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(f1.toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        assertThat(context.convertedPath(metadata).get()).isEqualTo(f2.toPath());
        assertThat(context.convertedPath(metadata).get()).exists();
    }

    @Test
    public void testSyncDoesntConvertNativeMarkdownRemotes() throws BatchProcessException, IOException {
        File f1 = folder.newFile("hello.md");
        Resources.save(f1.getPath(), "hello");

        Config config = new Config();
        config.remote().supportsNativeMarkdown(Optional.of(true));
        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(f1.toPath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
        assertThat(context.convertedPath(metadata)).isEmpty();
    }
}
