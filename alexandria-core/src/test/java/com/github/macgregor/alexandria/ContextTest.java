package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
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

    @Test
    public void contextMakesDisclaimerPathAbsoluteIfItExists() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.disclaimerFooterPath(Optional.of(Paths.get("footer.md")));
        context.makePathsAbsolute();
        assertThat(context.disclaimerFooterPath()).isPresent();
        assertThat(context.disclaimerFooterPath().get()).isAbsolute();
        assertThat(context.disclaimerFooterPath().get()).isEqualTo(Paths.get(folder.getRoot().getAbsolutePath(), "footer.md"));
    }

    @Test
    public void contextMakeAbsoluteIgnoresEmptyDisclaimerPath() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.disclaimerFooterPath(Optional.empty());
        context.makePathsAbsolute();
        assertThat(context.disclaimerFooterPath()).isEmpty();
    }

    @Test
    public void contextMakesDisclaimerPathRelativeIfItExists() throws IOException {
        Context context = TestData.minimalContext(folder);
        Path absPath = Paths.get(folder.getRoot().getAbsolutePath(), "footer.md");
        context.disclaimerFooterPath(Optional.of(absPath));
        context.makePathsRelative();
        assertThat(context.disclaimerFooterPath()).isPresent();
        assertThat(context.disclaimerFooterPath().get()).isRelative();
        assertThat(context.disclaimerFooterPath().get()).isEqualTo(Paths.get("footer.md"));
    }

    @Test
    public void contextMakeRelativeIgnoresEmptyDisclaimerPath() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.disclaimerFooterPath(Optional.empty());
        context.makePathsRelative();
        assertThat(context.disclaimerFooterPath()).isEmpty();
    }
}
