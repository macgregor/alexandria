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
        Config loaded = Alexandria.load("src/test/resources/config.yaml").config();
        assertThat(loaded.searchPath()).containsExactlyInAnyOrder(expected().searchPath().toArray(new String[]{}));
    }

    @Test
    public void testLoadOutputPath() throws IOException, URISyntaxException {
        Config loaded = Alexandria.load("src/test/resources/config.yaml").config();
        assertThat(loaded.output()).isEqualTo(expected().output());
    }

    @Test
    public void testLoadInclude() throws IOException, URISyntaxException {
        Config loaded = Alexandria.load("src/test/resources/config.yaml").config();
        assertThat(loaded.include()).containsExactlyInAnyOrder(expected().include().toArray(new String[]{}));
    }

    @Test
    public void testLoadExclude() throws IOException, URISyntaxException {
        Config loaded = Alexandria.load("src/test/resources/config.yaml").config();
        assertThat(loaded.exclude().get()).containsExactlyInAnyOrder(expected().exclude().get().toArray(new String[]{}));
    }

    @Test
    public void testLoadDefaultTags() throws IOException, URISyntaxException {
        Config loaded = Alexandria.load("src/test/resources/config.yaml").config();
        assertThat(loaded.defaultTags().get()).containsExactlyInAnyOrder(expected().defaultTags().get().toArray(new String[]{}));
    }

    @Test
    public void testLoadRemoteClass() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.clazz()).isEqualTo(expected().remote().clazz());
    }

    @Test
    public void testLoadRemoteBaseUrl() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.baseUrl()).isEqualTo(expected().remote().baseUrl());
    }

    @Test
    public void testLoadRemoteUsername() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.username()).isEqualTo(expected().remote().username());
    }

    @Test
    public void testLoadRemotePassword() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.password()).isEqualTo(expected().remote().password());
    }

    @Test
    public void testLoadRemoteSupportNativeMarkdown() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.supportsNativeMarkdown()).isEqualTo(expected().remote().supportsNativeMarkdown());
    }

    @Test
    public void testLoadRemoteDateTimeFormat() throws IOException, URISyntaxException {
        Config.RemoteConfig loaded = Alexandria.load("src/test/resources/config.yaml").config().remote();
        assertThat(loaded.datetimeFormat()).isEqualTo(expected().remote().datetimeFormat());
    }

    @Test
    public void testLoadMetadataSourcePath() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.sourcePath().toAbsolutePath()).isEqualTo(expected().metadata().get().get(0).sourcePath().toAbsolutePath());
    }

    @Test
    public void testLoadMetadataTitle() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.title()).isEqualTo(expected().metadata().get().get(0).title());
    }

    @Test
    public void testLoadMetadataRemoteUri() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.remoteUri()).isEqualTo(expected().metadata().get().get(0).remoteUri());
    }

    @Test
    public void testLoadMetadataCreatedOn() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.createdOn()).isEqualTo(expected().metadata().get().get(0).createdOn());
    }

    @Test
    public void testLoadMetadataLastUpdated() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.lastUpdated()).isEqualTo(expected().metadata().get().get(0).lastUpdated());
    }

    @Test
    public void testLoadMetadataDeletedOn() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.deletedOn()).isEqualTo(expected().metadata().get().get(0).deletedOn());
    }

    @Test
    public void testLoadMetadataSourceChecksum() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.sourceChecksum()).isEqualTo(expected().metadata().get().get(0).sourceChecksum());
    }

    @Test
    public void testLoadMetadataTags() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.tags()).isEqualTo(expected().metadata().get().get(0).tags());
    }

    @Test
    public void testLoadMetadataExtraProps() throws IOException, URISyntaxException {
        Config.DocumentMetadata loaded = Alexandria.load("src/test/resources/config.yaml").config().metadata().get().get(0);
        assertThat(loaded.extraProps()).isEqualTo(expected().metadata().get().get(0).extraProps());
    }

    @Test
    public void testLoadHandlesEmptyFile() throws IOException {
        Config config = Alexandria.load("src/test/resources/empty.yaml").config();
        assertThat(config).isNotNull();
    }

    @Test
    public void testLoadHandlesMissingFile() throws IOException {
        String doesntExist = new File(folder.getRoot(), "missing_config.yaml").getPath();
        Context context = Alexandria.load(doesntExist);
        Config config = context.config();
        assertThat(config).isNotNull();
        assertThat(context.configPath()).isEqualTo(Paths.get(doesntExist));
    }

    private Config expected() throws URISyntaxException {
        Config config = new Config();
        config.searchPath(Arrays.asList("src/test/resources"));
        config.output(Optional.of("src/test/resources"));
        config.include(Arrays.asList("*.txt"));
        config.exclude(Optional.of(Arrays.asList("*.md")));
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
