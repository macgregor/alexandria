package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigSaveTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testDontSaveDefaultTags() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().defaultTags(expected().defaultTags());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.defaultTags().get()).isEmpty();
    }

    @Test
    public void testDontSaveRemoteClass() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().clazz(expected().remote().clazz());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.remote().clazz()).isEqualTo(new Config().remote().clazz());
    }

    @Test
    public void testDontSaveRemoteBaseUrl() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().baseUrl(expected().remote().baseUrl());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.remote().baseUrl()).isEqualTo(Optional.empty());
    }

    @Test
    public void testDontSaveRemoteUsername() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().username(expected().remote().username());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.remote().username()).isEmpty();
    }

    @Test
    public void testDontSaveRemotePassword() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().password(expected().remote().password());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.remote().password()).isEqualTo(Optional.empty());
    }

    @Test
    public void testSaveRemoteDateTimeFormat() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().datetimeFormat(expected().remote().datetimeFormat());

        Config reloaded = saveAndReload(context.config());
        assertThat(reloaded.remote().datetimeFormat()).isEqualTo(Config.ALEXANDRIA_DATETIME_PATTERN);
    }

    @Test
    public void testSaveMetadataSourcePath() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.sourcePath(Resources.absolutePath(context.configPath().getParent(), expected.sourcePath()));

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.sourcePath().toAbsolutePath())
                .isEqualTo(Resources.absolutePath(context.configPath().getParent(), expected.sourcePath()));
    }

    @Test
    public void testSaveMetadataTitle() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.title(expected.title());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.title()).isEqualTo(expected.title());
    }

    @Test
    public void testSaveMetadataRemoteUri() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.remoteUri(expected.remoteUri());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.remoteUri()).isEqualTo(expected.remoteUri());
    }

    @Test
    public void testSaveMetadataLastUpdated() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.lastUpdated(expected.lastUpdated());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.lastUpdated()).isEqualTo(expected.lastUpdated());
    }

    @Test
    public void testSaveMetadataCreatedOn() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.createdOn(expected.createdOn());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.createdOn()).isEqualTo(expected.createdOn());
    }

    @Test
    public void testSaveMetadataDeletedOn() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.deletedOn(expected.deletedOn());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.deletedOn()).isEqualTo(expected.deletedOn());
    }

    @Test
    public void testSaveMetadataSourceChecksum() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.sourceChecksum(expected.sourceChecksum());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.sourceChecksum()).isEqualTo(expected.sourceChecksum());
    }

    @Test
    public void testSaveMetadataTags() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.tags(expected.tags());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.tags()).isEqualTo(expected.tags());
    }

    @Test
    public void testSaveMetadataExtraProps() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.extraProps(expected.extraProps());

        Config.DocumentMetadata actual = saveAndReload(context.config()).metadata().get().get(0);
        assertThat(actual.extraProps()).isEqualTo(expected.extraProps());
    }

    @Test
    public void testSaveMetadataMultiDocument() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata1 = context.config().metadata().get().get(0);
        Config.DocumentMetadata metadata2 = TestData.minimalDocumentMetadata(context, folder);
        Context.save(context);
    }

    private Config saveAndReload(Config config) throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config(config);
        Context.save(context);

        Config reloaded = Context.load(context.configPath().toString()).config();
        assertThat(reloaded).isNotNull();
        return reloaded;
    }

    private Config expected() throws URISyntaxException {
        Config config = new Config();
        config.defaultTags(Optional.of(Arrays.asList("foo", "bar")));

        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz("com.foo.bar.SomeRemoteClass");
        remoteConfig.baseUrl(Optional.of("http://www.google.com"));
        remoteConfig.username(Optional.of("matt"));
        remoteConfig.password(Optional.of("password"));
        remoteConfig.datetimeFormat("yyyy-MM-dd'override'HH:mm:ss.SSSZ");
        config.remote(remoteConfig);

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("README.md"));
        metadata.title("Readme");
        metadata.remoteUri(Optional.of(new URI("http://www.google.com/readme.html")));
        metadata.createdOn(Optional.of(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN))));
        metadata.lastUpdated(Optional.of(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN))));
        metadata.deletedOn(Optional.of(ZonedDateTime.parse("2018-06-23T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN))));
        metadata.sourceChecksum(Optional.of(1234567L));
        metadata.tags(Optional.of(Arrays.asList("baz")));
        metadata.extraProps(Optional.of(Collections.singletonMap("foo", "bar")));
        config.metadata(Optional.of(Arrays.asList(metadata)));

        return config;
    }
}
