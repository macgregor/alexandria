package com.github.macgregor.alexandria;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentMetadataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void getExtraPropertyReturnsNullOnMissingProperty(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        assertThat(metadata.getExtraProperty("foo")).isNull();
    }

    @Test
    public void testEquals(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");

        assertThat(metadata).isEqualTo(other);
    }

    @Test
    public void testNotEquals(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title1");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title2");

        assertThat(metadata).isNotEqualTo(other);
    }

    @Test
    public void testEqualsOnlyCaresAboutTitleAndSourcePath(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");
        metadata.tags(Optional.of(Arrays.asList("foo")));

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");
        metadata.tags(Optional.of(Arrays.asList("bar")));

        assertThat(metadata).isEqualTo(other);
    }

    @Test
    public void testToString(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");

        assertThat(metadata.toString()).isEqualTo(other.toString());
    }

    @Test
    public void determinesStateDeleted() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        metadata.deletedOn(Optional.of(ZonedDateTime.now()));
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.DELETED);
    }

    @Test
    public void determinesStateCurrent() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.of(FileUtils.checksumCRC32(context.resolveRelativePath(metadata.sourcePath()).toFile())));
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.CURRENT);
    }

    @Test
    public void determinesStateCreate() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.empty());
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.CREATE);
    }

    @Test
    public void determinesStateDelete() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        metadata.extraProps(Optional.of(Collections.singletonMap("delete", "true")));
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.DELETE);
    }

    @Test
    public void determinesStateDeleteSourcePathNotPresent() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForDelete(context, folder);
        metadata.extraProps(Optional.empty());
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.DELETE);
    }

    @Test
    public void determinesStateUpdateSourceChecksumDifference() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.of(-1L));
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.UPDATE);
    }

    @Test
    public void determinesStateUpdateConvertedChecksumDifference() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata documentForUpdate = TestData.documentForUpdate(context, folder);
        documentForUpdate.remoteUri(Optional.of(new URI("foo")));

        documentForUpdate.sourceChecksum(Optional.of(FileUtils.checksumCRC32(documentForUpdate.sourcePath().toFile())));
        documentForUpdate.convertedChecksum(Optional.of(-1L));
        assertThat(documentForUpdate.determineState()).isEqualTo(Config.DocumentMetadata.State.UPDATE);
    }

    @Test
    public void determinesStateCurrentConvertedPathMissing() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata documentForUpdate = TestData.documentForUpdate(context, folder);
        documentForUpdate.remoteUri(Optional.of(new URI("foo")));

        documentForUpdate.sourceChecksum(Optional.of(FileUtils.checksumCRC32(documentForUpdate.sourcePath().toFile())));
        documentForUpdate.convertedPath(Optional.empty());
        assertThat(documentForUpdate.determineState()).isEqualTo(Config.DocumentMetadata.State.CURRENT);
    }

    @Test
    public void determinesStateCurrentSourceChecksumMissing() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.sourceChecksum(Optional.empty());
        assertThat(metadata.determineState()).isEqualTo(Config.DocumentMetadata.State.CURRENT);
    }
}
