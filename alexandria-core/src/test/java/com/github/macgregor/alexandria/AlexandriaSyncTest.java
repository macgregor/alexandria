package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import com.github.macgregor.alexandria.remotes.NoopRemote;
import com.github.macgregor.alexandria.remotes.Remote;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class AlexandriaSyncTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSyncWithJiveRemote() throws AlexandriaException {
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
    public void testSyncWithNoopRemote() throws AlexandriaException {
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
                .isInstanceOf(AlexandriaException.class)
                .hasMessageContaining("com.github.macgregor.alexandria.remotes.NotAThing");
    }

    @Test
    public void testSyncCalculatesChecksum() throws BatchProcessException, IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.sourceChecksum(Optional.empty());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context);
        alexandriaSync.syncWithRemote();
        assertThat(metadata.sourceChecksum()).isPresent();
    }

    @Test
    public void testSyncCreatesDocument() throws BatchProcessException, IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Remote remote = mock(NoopRemote.class);
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).create(context, metadata);
    }

    @Test
    public void testSyncDeletesDocument() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        Remote remote = mock(NoopRemote.class);
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).delete(context, metadata);
    }

    @Test
    public void testSyncUpdatesDocument() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.of(-1L));
        Remote remote = mock(NoopRemote.class);
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).update(context, metadata);
    }

    @Test
    public void testSyncIgnoresAlreadyDeleted() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        metadata.extraProps(Optional.empty());
        metadata.deletedOn(Optional.of(ZonedDateTime.now()));
        Remote remote = mock(NoopRemote.class);
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).delete(context, metadata);
        verify(remote, times(0)).update(context, metadata);
        verify(remote, times(0)).create(context, metadata);
    }

    @Test
    public void testSyncIgnoresCurrent() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.completeDocumentMetadata(context, folder, "foo.md");
        metadata.extraProps(Optional.empty());
        metadata.deletedOn(Optional.empty());
        Remote remote = mock(NoopRemote.class);
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).delete(context, metadata);
        verify(remote, times(0)).update(context, metadata);
        verify(remote, times(0)).create(context, metadata);
    }

    @Test
    public void testSyncNeedsConversionFalseWhenRemoteSuportsMarkdown() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.config().remote().supportsNativeMarkdown(true);
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isFalse();
    }

    @Test
    public void testSyncNeedsConversionTrueWhenConvertedCacheHitButFileMissing() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.convertedPath(metadata, AlexandriaConvert.convertedPath(context, metadata));
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isTrue();
    }

    @Test
    public void testSyncNeedsConversionFalseWhenConvertedCacheHitAndFileExists() throws IOException, URISyntaxException {
        Context context = TestData.completeContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isFalse();
    }

    @Test
    public void testSyncNeedsConversionTrueWhenConvertedCacheHitAndFileExistsButWrongChecksum() throws IOException, URISyntaxException {
        Context context = TestData.completeContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.convertedChecksum(Optional.of(-1l));
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isTrue();
    }

    @Test
    public void testSyncNeedsConversionTrueWhenConvertedCacheMissAndFileMissing() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.convertedPaths(new HashMap<>());
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isTrue();
    }

    @Test
    public void testSyncNeedsConversionFalseWhenConvertedCacheMissAndFileExists() throws IOException, URISyntaxException {
        Context context = TestData.completeContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.convertedPaths(new HashMap<>());
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isFalse();
    }

    @Test
    public void testSyncNeedsConversionTrueWhenConvertedCacheMissAndFileExistsWithWrongChecksum() throws IOException, URISyntaxException {
        Context context = TestData.completeContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.convertedPaths(new HashMap<>());
        metadata.convertedChecksum(Optional.of(-1l));
        assertThat(AlexandriaSync.needsConversion(context, metadata)).isTrue();
    }
}
