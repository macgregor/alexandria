package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
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

public class ConfigLoadTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLoadSearchPath() throws IOException, URISyntaxException {
        Config loaded = Config.load("src/test/resources/config.yaml");
        assertThat(loaded.searchPath()).containsExactlyInAnyOrder(expected().searchPath().toArray(new String[]{}));
    }

    @Test
    public void testLoadOutputPath() throws IOException, URISyntaxException {
        Config loaded = Config.load("src/test/resources/config.yaml");
        assertThat(loaded.output()).isEqualTo(expected().output());
    }

    @Test
    public void testLoadInclude() throws IOException, URISyntaxException {
        Config loaded = Config.load("src/test/resources/config.yaml");
        assertThat(loaded.include()).containsExactlyInAnyOrder(expected().include().toArray(new String[]{}));
    }

    @Test
    public void testLoadExclude() throws IOException, URISyntaxException {
        Config loaded = Config.load("src/test/resources/config.yaml");
        assertThat(loaded.exclude().get()).containsExactlyInAnyOrder(expected().exclude().get().toArray(new String[]{}));
    }

    @Test
    public void testLoadDefaultTags() throws IOException, URISyntaxException {
        Config loaded = Config.load("src/test/resources/config.yaml");
        assertThat(loaded.defaultTags().get()).containsExactlyInAnyOrder(expected().defaultTags().get().toArray(new String[]{}));
    }

    @Test
    public void testLoadRemoteBaseUrl() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Config.load("src/test/resources/config.yaml").remote().get();
        assertThat(loaded.baseUrl()).isEqualTo(expected().remote().get().baseUrl());
    }

    @Test
    public void testLoadRemoteUsername() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Config.load("src/test/resources/config.yaml").remote().get();
        assertThat(loaded.username()).isEqualTo(expected().remote().get().username());
    }

    @Test
    public void testLoadRemotePassword() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Config.load("src/test/resources/config.yaml").remote().get();
        assertThat(loaded.password()).isEqualTo(expected().remote().get().password());
    }

    @Test
    public void testLoadRemoteSupportNativeMarkdown() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Config.load("src/test/resources/config.yaml").remote().get();
        assertThat(loaded.supportsNativeMarkdown()).isEqualTo(expected().remote().get().supportsNativeMarkdown());
    }

    @Test
    public void testLoadRemoteDateTimeFormat() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Config.load("src/test/resources/config.yaml").remote().get();
        assertThat(loaded.datetimeFormat()).isEqualTo(expected().remote().get().datetimeFormat());
    }

    @Test
    public void testLoadMetadataSourcePath() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.sourcePath().toAbsolutePath()).isEqualTo(expected().metadata().get().get(0).sourcePath().toAbsolutePath());
    }

    @Test
    public void testLoadMetadataTitle() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.title()).isEqualTo(expected().metadata().get().get(0).title());
    }

    @Test
    public void testLoadMetadataRemoteUri() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.remoteUri()).isEqualTo(expected().metadata().get().get(0).remoteUri());
    }

    @Test
    public void testLoadMetadataCreatedOn() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.createdOn()).isEqualTo(expected().metadata().get().get(0).createdOn());
    }

    @Test
    public void testLoadMetadataLastUpdated() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.lastUpdated()).isEqualTo(expected().metadata().get().get(0).lastUpdated());
    }

    @Test
    public void testLoadMetadataDeletedOn() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.deletedOn()).isEqualTo(expected().metadata().get().get(0).deletedOn());
    }

    @Test
    public void testLoadMetadataSourceChecksum() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.sourceChecksum()).isEqualTo(expected().metadata().get().get(0).sourceChecksum());
    }

    @Test
    public void testLoadMetadataTags() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.tags()).isEqualTo(expected().metadata().get().get(0).tags());
    }

    @Test
    public void testLoadMetadataExtraProps() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Config.load("src/test/resources/config.yaml").metadata().get().get(0);
        assertThat(loaded.extraProps()).isEqualTo(expected().metadata().get().get(0).extraProps());
    }

    @Test
    public void testLoadHandlesEmptyFile() throws IOException {
        Config config = Config.load("src/test/resources/empty.yaml");
        assertThat(config).isNotNull();
    }

    @Test
    public void testLoadHandlesMissingFile() throws IOException {
        String doesntExist = new File(folder.getRoot(), "missing_config.yaml").getPath();
        Config config = Config.load(doesntExist);
        assertThat(config).isNotNull();
        assertThat(config.configPath()).isEqualTo(Paths.get(doesntExist));
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
