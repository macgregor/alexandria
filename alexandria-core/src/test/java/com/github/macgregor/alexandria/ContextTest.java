package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.markdown.NoopMarkdownConverter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testContextMakePathsAbsoluteNullConfigPath() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.configPath = null;
        assertThat(context.makePathsAbsolute()).isEqualTo(context);
    }

    @Test
    public void testContextAddsMetadataInitializesMetadataListIfNecessary() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config.metadata = Optional.empty();
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(folder);
        context.addMetadata(metadata);
        assertThat(context.config.metadata).isPresent();
        assertThat(context.config.metadata.get()).contains(metadata);
    }

    @Test
    public void testContextIsIndexedReturnsEmptyWhenNoMetadata() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config.metadata = Optional.empty();
        assertThat(context.isIndexed(folder.getRoot().toPath())).isEmpty();
    }

    @Test
    public void testContextMetadataCountReturnsZeroWhenNoMetadata() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config.metadata = Optional.empty();
        assertThat(context.documentCount()).isEqualTo(0);
    }

    @Test
    public void testContextMetadataCount() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config.metadata = Optional.empty();
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(folder);
        context.addMetadata(metadata);
        assertThat(context.documentCount()).isEqualTo(1);
    }

    @Test
    public void testContextConvertedPathMiss() throws IOException {
        Context context = new Context();
        assertThat(context.convertedPath(new Config.DocumentMetadata())).isEmpty();
    }

    @Test
    public void testContextConvertedPathHit() throws IOException {
        Context context = new Context();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        context.convertedPath(metadata, Paths.get("foo"));
        assertThat(context.convertedPath(metadata)).isPresent();
        assertThat(context.convertedPath(metadata).get()).isEqualTo(Paths.get("foo"));
    }
}
