package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
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
}
