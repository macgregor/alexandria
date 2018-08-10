package com.github.macgregor.alexandria;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class AlexandriaMetadata{
    private Path source;
    private String title;
    private Optional<Path> converted = Optional.empty();
    private Optional<URI> remote = Optional.empty();
    private Optional<List<String>> tags = Optional.of(new ArrayList<>());
    private Optional<ZonedDateTime> createdOn = Optional.empty();
    private Optional<ZonedDateTime> lastUpdated = Optional.empty();
    private Optional<Map<String, String>> extra = Optional.of(new HashMap<>());
}