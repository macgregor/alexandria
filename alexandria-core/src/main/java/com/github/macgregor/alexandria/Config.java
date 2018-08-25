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
        private Optional<Boolean> supportsNativeMarkdown = Optional.of(false);

        @JsonProperty
        private Optional<String> datetimeFormat = Optional.of(ALEXANDRIA_DATETIME_PATTERN);
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
    }
}
