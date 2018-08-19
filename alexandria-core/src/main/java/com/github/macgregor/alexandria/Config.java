package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class Config {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @JsonProperty
    private RemoteConfig remote = new RemoteConfig();

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

    public RemoteConfig remote() {
        return remote;
    }

    public void remote(RemoteConfig remote) {
        this.remote = remote;
    }

    public Optional<List<DocumentMetadata>> metadata() {
        return metadata;
    }

    public void metadata(Optional<List<DocumentMetadata>> metadata) {
        this.metadata = metadata;
    }

    public static class RemoteConfig{
        @JsonProperty("class")
        private String clazz = "com.github.macgregor.alexandria.remotes.NoopRemote";

        @JsonProperty
        private Optional<String> baseUrl = Optional.empty();

        @JsonProperty
        private Optional<String> username = Optional.empty();

        @JsonProperty
        private Optional<String> password = Optional.empty();

        @JsonProperty
        private Optional<Boolean> supportsNativeMarkdown = Optional.of(false);

        @JsonProperty
        private Optional<String> datetimeFormat = Optional.of(ALEXANDRIA_DATETIME_PATTERN);

        public String clazz() {
            return clazz;
        }

        public void clazz(String clazz) {
            this.clazz = clazz;
        }

        public Optional<String> baseUrl() {
            return baseUrl;
        }

        public void baseUrl(Optional<String> baseUrl) {
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

        public Optional<String> username() {
            return username;
        }

        public void username(Optional<String> username) {
            this.username = username;
        }

        public Optional<String> password() {
            return password;
        }

        public void password(Optional<String> password) {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocumentMetadata metadata = (DocumentMetadata) o;
            return Objects.equals(sourcePath, metadata.sourcePath) &&
                    Objects.equals(title, metadata.title);
        }

        @Override
        public int hashCode() {

            return Objects.hash(sourcePath, title);
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
                    '}';
        }
    }
}
