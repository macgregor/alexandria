package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
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
    public void testSaveDefaultTags() throws IOException, URISyntaxException {
        Config config = new Config();
        config.defaultTags(expected().defaultTags());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.defaultTags().get()).containsExactlyInAnyOrder(expected().defaultTags().get().toArray(new String[]{}));
    }

    @Test
    public void testSaveRemoteClass() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz(expected().remote().clazz());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().clazz()).isEqualTo(expected().remote().clazz());
    }

    @Test
    public void testSaveRemoteBaseUrl() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.baseUrl(expected().remote().baseUrl());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().baseUrl()).isEqualTo(expected().remote().baseUrl());
    }

    @Test
    public void testSaveRemoteUsername() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.username(expected().remote().username());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().username()).isEqualTo(expected().remote().username());
    }

    @Test
    public void testSaveRemotePassword() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.password(expected().remote().password());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().password()).isEqualTo(expected().remote().password());
    }

    @Test
    public void testSaveRemoteSupportNativeMarkdown() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.supportsNativeMarkdown(expected().remote().supportsNativeMarkdown());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().supportsNativeMarkdown()).isEqualTo(expected().remote().supportsNativeMarkdown());
    }

    @Test
    public void testSaveRemoteDateTimeFormat() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.datetimeFormat(expected().remote().datetimeFormat());
        config.remote(remoteConfig);

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().datetimeFormat()).isEqualTo(expected().remote().datetimeFormat());
    }

    @Test
    public void testSaveMetadataSourcePath() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.sourcePath(expected.sourcePath());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.sourcePath().toAbsolutePath()).isEqualTo(expected.sourcePath().toAbsolutePath());
    }

    @Test
    public void testSaveMetadataTitle() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.title(expected.title());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.title()).isEqualTo(expected.title());
    }

    @Test
    public void testSaveMetadataRemoteUri() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.remoteUri(expected.remoteUri());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.remoteUri()).isEqualTo(expected.remoteUri());
    }

    @Test
    public void testSaveMetadataLastUpdated() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.lastUpdated(expected.lastUpdated());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.lastUpdated()).isEqualTo(expected.lastUpdated());
    }

    @Test
    public void testSaveMetadataCreatedOn() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.createdOn(expected.createdOn());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.createdOn()).isEqualTo(expected.createdOn());
    }

    @Test
    public void testSaveMetadataDeletedOn() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.deletedOn(expected.deletedOn());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.deletedOn()).isEqualTo(expected.deletedOn());
    }

    @Test
    public void testSaveMetadataSourceChecksum() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.sourceChecksum(expected.sourceChecksum());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.sourceChecksum()).isEqualTo(expected.sourceChecksum());
    }

    @Test
    public void testSaveMetadataTags() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.tags(expected.tags());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.tags()).isEqualTo(expected.tags());
    }

    @Test
    public void testSaveMetadataExtraProps() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Config.DocumentMetadata expected = expected().metadata().get().get(0);
        metadata.extraProps(expected.extraProps());
        config.metadata(Optional.of(Arrays.asList(metadata)));

        Config.DocumentMetadata actual = saveAndReload(config).metadata().get().get(0);
        assertThat(actual.extraProps()).isEqualTo(expected.extraProps());
    }

    @Test
    public void testSaveMetadataMultiDocument() throws IOException {
        Config config = new Config();
        Config.DocumentMetadata metadata1 = new Config.DocumentMetadata();
        metadata1.sourcePath(Paths.get(folder.getRoot().toString(),"README.md"));
        Config.DocumentMetadata metadata2 = new Config.DocumentMetadata();
        metadata1.sourcePath(Paths.get(folder.getRoot().toString(),"README2.md"));
        config.metadata(Optional.of(Arrays.asList(metadata1, metadata2)));

        Path path = folder.newFile().toPath();
        Context context = new Context();
        context.configPath(path);
        context.config(config);
        Alexandria.save(context);
    }

    private Config saveAndReload(Config config) throws IOException {
        Path path = folder.newFile().toPath();
        Context context = new Context();
        context.configPath(path);
        context.config(config);
        Alexandria.save(context);

        Config reloaded = Alexandria.load(path.toString()).config();
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
        remoteConfig.supportsNativeMarkdown(Optional.of(true));
        remoteConfig.datetimeFormat(Optional.of("yyyy-MM-dd'override'HH:mm:ss.SSSZ"));
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
