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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLoadDefaultTags() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.defaultTags()).isPresent();
        assertThat(config.defaultTags().get()).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void testLoadRemotesBaseUrl() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.remote()).isPresent();
        Config.RemoteConfig remoteConfig = config.remote().get();
        assertThat(remoteConfig.baseUrl()).isEqualTo("http://www.google.com");
    }

    @Test
    public void testLoadRemotesUsername() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.remote()).isPresent();
        Config.RemoteConfig remoteConfig = config.remote().get();
        assertThat(remoteConfig.username()).isEqualTo("matt");
    }

    @Test
    public void testLoadRemotesPassword() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.remote()).isPresent();
        Config.RemoteConfig remoteConfig = config.remote().get();
        assertThat(remoteConfig.password()).isEqualTo("password");
    }

    @Test
    public void testLoadRemotesNativeMarkdown() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.remote()).isPresent();
        Config.RemoteConfig remoteConfig = config.remote().get();
        assertThat(remoteConfig.supportsNativeMarkdown().get()).isTrue();
    }

    @Test
    public void testLoadRemotesDateTimeFormat() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.remote()).isPresent();
        Config.RemoteConfig remoteConfig = config.remote().get();
        assertThat(remoteConfig.datetimeFormat().get()).isEqualTo("yyyy-MM-dd'override'HH:mm:ss.SSSZ");
    }

    @Test
    public void testLoadMetadataCreatedDateTimeIsConverter() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.createdOn()).isPresent();
        ZonedDateTime expected = ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        assertThat(metadata.createdOn().get()).isEqualTo(expected);
    }

    @Test
    public void testLoadMetadataLastUpdatedDateTimeIsConverter() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.createdOn()).isPresent();
        ZonedDateTime expected = ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        assertThat(metadata.lastUpdated().get()).isEqualTo(expected);
    }

    @Test
    public void testLoadMetadataSource() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.sourcePath()).isEqualTo(Paths.get("README.md"));
    }

    @Test
    public void testLoadMetadataTitle() throws IOException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.title()).isEqualTo("Readme");
    }

    @Test
    public void testLoadMetadataRemote() throws IOException, URISyntaxException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.remoteUri().get()).isEqualTo(new URI("http://www.google.com/readme.html"));
    }

    @Test
    public void testLoadMetadataTags() throws IOException, URISyntaxException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.tags().get()).containsExactlyInAnyOrder("baz");
    }

    @Test
    public void testLoadMetadataExtraProps() throws IOException, URISyntaxException {
        Config config = Config.load("src/test/resources/config.yaml");
        assertThat(config.metadata()).isPresent();
        Config.DocumentMetadata metadata = config.metadata().get().get(0);
        assertThat(metadata.extraProps().get()).containsExactly(new Map.Entry<String, String>() {
            @Override
            public String getKey() {
                return "foo";
            }

            @Override
            public String getValue() {
                return "bar";
            }

            @Override
            public String setValue(String value) {
                return value;
            }
        });
    }

    @Test
    public void testConfigDefaultsForDefaultTags() throws IOException {
        Config config = Config.load("src/test/resources/empty.yaml");
        assertThat(config.defaultTags()).isPresent();
        assertThat(config.defaultTags().get()).isEmpty();
    }

    @Test
    public void testConfigDefaultsForRemotes() throws IOException {
        Config config = Config.load("src/test/resources/empty.yaml");
        assertThat(config.remote()).isEmpty();
    }

    @Test
    public void testConfigSave() throws IOException {
        Config config = Config.load("src/test/resources/empty.yaml");
        config.defaultTags(Optional.of(Arrays.asList("updated")));
        config.configPath(folder.newFile().toPath());
        Config.save(config);
        Config reloaded = Config.load(config.configPath().toString());
        assertThat(reloaded.defaultTags().get()).containsExactlyInAnyOrder("updated");

    }
}
