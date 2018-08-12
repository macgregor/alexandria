package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class AlexandriaConfig {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @JsonProperty
    private Path searchPath;

    @JsonProperty
    private List<String> include = new ArrayList<>(Arrays.asList("**.md"));

    @JsonProperty
    private Optional<List<String>> exclude = Optional.of(new ArrayList<>());

    @JsonProperty
    private Optional<List<RemoteConfig>> remotes = Optional.of(new ArrayList<>());

    @JsonProperty
    private Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());

    public Optional<List<String>> defaultTags() {
        return defaultTags;
    }

    public void defaultTags(Optional<List<String>> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public Optional<List<RemoteConfig>> remotes() {
        return remotes;
    }

    public void remotes(Optional<List<RemoteConfig>> remotes) {
        this.remotes = remotes;
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

    public Path searchPath() {
        return searchPath;
    }

    public void searchPath(Path searchPath) {
        this.searchPath = searchPath;
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
        private Optional<Boolean> enabled = Optional.of(true);

        @JsonProperty
        private Optional<String> datetimeFormat = Optional.of(ALEXANDRIA_DATETIME_PATTERN);

        @JsonProperty
        private Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

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

        public Optional<Boolean> enabled() {
            return enabled;
        }

        public void enabled(Optional<Boolean> enabled) {
            this.enabled = enabled;
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

        public Optional<List<DocumentMetadata>> metadata() {
            return metadata;
        }

        public void metadata(Optional<List<DocumentMetadata>> metadata) {
            this.metadata = metadata;
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
        private Optional<ZonedDateTime> createdOn = Optional.empty();

        @JsonProperty
        private Optional<ZonedDateTime> lastUpdated = Optional.empty();

        @JsonProperty
        private Optional<ZonedDateTime> deletedOn = Optional.empty();

        @JsonProperty
        private Optional<Map<String, String>> extraProps = Optional.of(new HashMap<>());

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
    }

    /**
     * Config parsing code
     */

    @JsonIgnore
    private Path propertiesPath;

    public static AlexandriaConfig load(String filePath) throws IOException {
        Path propertiesPath = Resources.path(filePath, true);
        AlexandriaConfig config = Jackson.yamlMapper().readValue(propertiesPath.toFile(), AlexandriaConfig.class);
        config.propertiesPath = propertiesPath;
        return config;
    }

    public static void save(AlexandriaConfig config) throws IOException {
        Jackson.yamlMapper().writeValue(config.propertiesPath.toFile(), config);
    }

    public Path propertiesPath() {
        return propertiesPath;
    }

    public void propertiesPath(Path propertiesPath) {
        this.propertiesPath = propertiesPath;
    }
}
