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
    public void testSaveSearchPath() throws IOException, URISyntaxException {
        Config config = new Config();
        config.searchPath(expected().searchPath());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.searchPath()).containsExactlyInAnyOrder(expected().searchPath().toArray(new String[]{}));
    }

    @Test
    public void testSaveOutputPath() throws IOException, URISyntaxException {
        Config config = new Config();
        config.output(expected().output());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.output().get()).isEqualTo(expected().output().get());
    }

    @Test
    public void testSaveInclude() throws IOException, URISyntaxException {
        Config config = new Config();
        config.include(expected().include());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.include()).containsExactlyInAnyOrder(expected().include().toArray(new String[]{}));
    }

    @Test
    public void testSaveExclude() throws IOException, URISyntaxException {
        Config config = new Config();
        config.exclude(expected().exclude());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.exclude().get()).containsExactlyInAnyOrder(expected().exclude().get().toArray(new String[]{}));
    }

    @Test
    public void testSaveDefaultTags() throws IOException, URISyntaxException {
        Config config = new Config();
        config.defaultTags(expected().defaultTags());

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.defaultTags().get()).containsExactlyInAnyOrder(expected().defaultTags().get().toArray(new String[]{}));
    }

    @Test
    public void testSaveRemoteBaseUrl() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.baseUrl(expected().remote().get().baseUrl());
        config.remotes(Optional.of(remoteConfig));

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().get().baseUrl()).isEqualTo(expected().remote().get().baseUrl());
    }

    @Test
    public void testSaveRemoteUsername() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.username(expected().remote().get().username());
        config.remotes(Optional.of(remoteConfig));

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().get().username()).isEqualTo(expected().remote().get().username());
    }

    @Test
    public void testSaveRemotePassword() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.password(expected().remote().get().password());
        config.remotes(Optional.of(remoteConfig));

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().get().password()).isEqualTo(expected().remote().get().password());
    }

    @Test
    public void testSaveRemoteSupportNativeMarkdown() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.supportsNativeMarkdown(expected().remote().get().supportsNativeMarkdown());
        config.remotes(Optional.of(remoteConfig));

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().get().supportsNativeMarkdown()).isEqualTo(expected().remote().get().supportsNativeMarkdown());
    }

    @Test
    public void testSaveRemoteDateTimeFormat() throws IOException, URISyntaxException {
        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.datetimeFormat(expected().remote().get().datetimeFormat());
        config.remotes(Optional.of(remoteConfig));

        Config reloaded = saveAndReload(config);
        assertThat(reloaded.remote().get().datetimeFormat()).isEqualTo(expected().remote().get().datetimeFormat());
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

    private Config saveAndReload(Config config) throws IOException {
        Path path = folder.newFile().toPath();
        config.configPath(path);
        Config.save(config);

        Config reloaded = Config.load(path.toString());
        assertThat(reloaded).isNotNull();
        return reloaded;
    }

    private Config expected() throws URISyntaxException {
        Config config = new Config();
        config.searchPath(Arrays.asList("src/test/resources"));
        config.output(Optional.of("src/test/resources"));
        config.include(Arrays.asList("*.txt"));
        config.exclude(Optional.of(Arrays.asList("*.md")));
        config.defaultTags(Optional.of(Arrays.asList("foo", "bar")));

        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.baseUrl("http://www.google.com");
        remoteConfig.username("matt");
        remoteConfig.password("password");
        remoteConfig.supportsNativeMarkdown(Optional.of(true));
        remoteConfig.datetimeFormat(Optional.of("yyyy-MM-dd'override'HH:mm:ss.SSSZ"));
        config.remotes(Optional.of(remoteConfig));

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
