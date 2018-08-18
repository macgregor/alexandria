package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class Config {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DEFAULT_CONFIG_FILE = ".alexandria";

    @JsonProperty
    private List<String> searchPath;

    @JsonProperty
    private Optional<String> outputPath = Optional.empty();

    @JsonProperty
    private List<String> include = new ArrayList<>(Arrays.asList("*.md"));

    @JsonProperty
    private Optional<List<String>> exclude = Optional.of(new ArrayList<>());

    @JsonProperty
    private Optional<RemoteConfig> remote = Optional.empty();

    @JsonProperty
    private Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

    @JsonProperty
    private Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());

    public Optional<List<String>> defaultTags() {
        return defaultTags;
    }

    public void defaultTags(Optional<List<String>> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public Optional<RemoteConfig> remote() {
        return remote;
    }

    public void remotes(Optional<RemoteConfig> remote) {
        this.remote = remote;
    }

    public List<String> include() {
        return include;
    }

    public void include(List<String> include) {
        this.include = include;
    }

    public Optional<List<String>> exclude() {
        return exclude;
    }

    public void exclude(Optional<List<String>> exclude) {
        this.exclude = exclude;
    }

    public List<String> searchPath() {
        return searchPath;
    }

    public void searchPath(List<String> searchPath) {
        this.searchPath = searchPath;
    }

    public Optional<String> output() {
        return outputPath;
    }

    public void output(Optional<String> outputPath) {
        this.outputPath = outputPath;
    }

    public Optional<List<DocumentMetadata>> metadata() {
        return metadata;
    }

    public void metadata(Optional<List<DocumentMetadata>> metadata) {
        this.metadata = metadata;
    }

    public static class RemoteConfig{
        @JsonProperty
        private String baseUrl;

        @JsonProperty
        private String username;

        @JsonProperty
        private String password;

        @JsonProperty
        private Optional<Boolean> supportsNativeMarkdown = Optional.of(false);

        @JsonProperty
        private Optional<String> datetimeFormat = Optional.of(ALEXANDRIA_DATETIME_PATTERN);

        public String baseUrl() {
            return baseUrl;
        }

        public void baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Optional<Boolean> supportsNativeMarkdown() {
            return supportsNativeMarkdown;
        }

        public void supportsNativeMarkdown(Optional<Boolean> nativeMarkdown) {
            this.supportsNativeMarkdown = nativeMarkdown;
        }

        public Optional<String> datetimeFormat() {
            return datetimeFormat;
        }

        public void datetimeFormat(Optional<String> datetimeFormat) {
            this.datetimeFormat = datetimeFormat;
        }

        public String username() {
            return username;
        }

        public void username(String username) {
            this.username = username;
        }

        public String password() {
            return password;
        }

        public void password(String password) {
            this.password = password;
        }

    }

    public static class DocumentMetadata{
        @JsonProperty
        private Path sourcePath;

        @JsonProperty
        private String title;

        @JsonProperty
        private Optional<URI> remoteUri = Optional.empty();

        @JsonProperty
        private Optional<List<String>> tags = Optional.of(new ArrayList<>());

        @JsonProperty
        private Optional<Long> sourceChecksum = Optional.empty();

        @JsonProperty
        private Optional<ZonedDateTime> createdOn = Optional.empty();

        @JsonProperty
        private Optional<ZonedDateTime> lastUpdated = Optional.empty();

        @JsonProperty
        private Optional<ZonedDateTime> deletedOn = Optional.empty();

        @JsonProperty
        private Optional<Map<String, String>> extraProps = Optional.of(new HashMap<>());

        @JsonIgnore
        private Optional<Path> convertedPath = Optional.empty();

        public Path sourcePath() {
            return sourcePath;
        }

        public void sourcePath(Path source) {
            this.sourcePath = source;
        }

        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public Optional<Path> convertedPath() {
            return convertedPath;
        }

        public void convertedPath(Optional<Path> convertedPath) {
            this.convertedPath = convertedPath;
        }

        public Optional<URI> remoteUri() {
            return remoteUri;
        }

        public void remoteUri(Optional<URI> remote) {
            this.remoteUri = remote;
        }

        public Optional<List<String>> tags() {
            return tags;
        }

        public void tags(Optional<List<String>> tags) {
            this.tags = tags;
        }

        public Optional<ZonedDateTime> createdOn() {
            return createdOn;
        }

        public void createdOn(Optional<ZonedDateTime> createdOn) {
            this.createdOn = createdOn;
        }

        public Optional<ZonedDateTime> lastUpdated() {
            return lastUpdated;
        }

        public void lastUpdated(Optional<ZonedDateTime> lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public Optional<ZonedDateTime> deletedOn() {
            return deletedOn;
        }

        public void deletedOn(Optional<ZonedDateTime> deletedOn) {
            this.deletedOn = deletedOn;
        }

        public Optional<Map<String, String>> extraProps() {
            return extraProps;
        }

        public void extraProps(Optional<Map<String, String>> extraProps) {
            this.extraProps = extraProps;
        }

        public Optional<Long> sourceChecksum() {
            return sourceChecksum;
        }

        public void sourceChecksum(Optional<Long> sourceChecksum) {
            this.sourceChecksum = sourceChecksum;
        }

        @Override
        public String toString() {
            return "DocumentMetadata{" +
                    "sourcePath=" + sourcePath +
                    ", title='" + title + '\'' +
                    ", remoteUri=" + remoteUri +
                    ", tags=" + tags +
                    ", sourceChecksum=" + sourceChecksum +
                    ", createdOn=" + createdOn +
                    ", lastUpdated=" + lastUpdated +
                    ", deletedOn=" + deletedOn +
                    ", extraProps=" + extraProps +
                    ", convertedPath=" + convertedPath +
                    '}';
        }
    }

    /**
     * Config parsing code
     */

    @JsonIgnore
    private Path configPath;

    @JsonIgnore
    private static Logger log = LoggerFactory.getLogger(Config.class);


    public static Config load(String filePath) throws IOException {
        Config config;
        Path path = Resources.path(filePath, false);
        if(path.toFile().exists()) {
            config = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            log.debug(String.format("Loaded configuration from %s.", path.toAbsolutePath().toString()));
        } else{
            config = new Config();
            log.debug(String.format("Created default configuration for new file %s.", path.toAbsolutePath().toString()));
        }
        config.configPath = path;
        return config;
    }

    public static void save(Config config) throws IOException {
        Jackson.yamlMapper().writeValue(config.configPath.toFile(), config);
        log.debug(String.format("Saved configuration to %s.", config.configPath.toAbsolutePath().toString()));
    }

    public Path configPath() {
        return configPath;
    }

    public void configPath(Path propertiesPath) {
        this.configPath = propertiesPath;
    }
}
