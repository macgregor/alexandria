package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class AlexandriaSyncTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).create(metadata);
    }

    @Test
    public void testSyncDoesntCreateDocumentWhenContentsBlank() throws BatchProcessException, IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Resources.save(metadata.sourcePath().toString(), "");
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).create(metadata);
    }

    @Test
    public void testSyncDeletesDocument() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).delete(metadata);
    }

    @Test
    public void testSyncUpdatesDocument() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.of(-1L));
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(1)).update(metadata);
    }

    @Test
    public void testSyncDoesntUpdateDocumentWhenContentsBlank() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Resources.save(metadata.sourcePath().toString(), "");
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.of(-1L));
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).update(metadata);
    }

    @Test
    public void testSyncIgnoresAlreadyDeleted() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        metadata.extraProps(Optional.empty());
        metadata.deletedOn(Optional.of(ZonedDateTime.now()));
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).delete(metadata);
        verify(remote, times(0)).update(metadata);
        verify(remote, times(0)).create(metadata);
    }

    @Test
    public void testSyncIgnoresCurrent() throws BatchProcessException, IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().metadata(Optional.of(new ArrayList<>()));
        Config.DocumentMetadata metadata = TestData.completeDocumentMetadata(context, folder);
        metadata.extraProps(Optional.empty());
        metadata.deletedOn(Optional.empty());
        Remote remote = spy(context.remote().get());
        AlexandriaSync alexandriaSync = new AlexandriaSync(context, remote);
        alexandriaSync.syncWithRemote();
        verify(remote, times(0)).delete(metadata);
        verify(remote, times(0)).update(metadata);
        verify(remote, times(0)).create(metadata);
    }
}
