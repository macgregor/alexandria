package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

@Data
@Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class Config {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @JsonProperty
    private RemoteConfig remote = new RemoteConfig();

    @JsonProperty
    private Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

    @JsonProperty
    private Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());

    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor @AllArgsConstructor
    @ToString
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
        private Boolean supportsNativeMarkdown = false;

        @JsonProperty
        private String datetimeFormat = ALEXANDRIA_DATETIME_PATTERN;

        @JsonProperty
        private Integer requestTimeout = 60;
    }

    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor
    @ToString @AllArgsConstructor
    public static class DocumentMetadata{
        @JsonProperty
        @NonNull private Path sourcePath;

        @JsonProperty
        @NonNull private String title;

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<URI> remoteUri = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<List<String>> tags = Optional.of(new ArrayList<>());

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Long> sourceChecksum = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Long> convertedChecksum = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> createdOn = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> lastUpdated = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> deletedOn = Optional.empty();

        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Map<String, String>> extraProps = Optional.of(new HashMap<>());
    }
}
