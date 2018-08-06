package com.github.macgregor.alexandria;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class DocumentMetadata {

    public static final String METADATA_START = "<!--- alexandria";
    public static final String METADATA_END = "-->";
    public static final String META_DATA_KEY_VALUE_SEP = ":";
    public static final String META_DATA_VALUE_LIST_SEP = ",";

    private Path source;
    private String title;
    private Optional<Path> converted;
    private Optional<URI> remote;
    private Optional<List<String>> tags;
    private Optional<ZonedDateTime> createdOn;
    private Optional<ZonedDateTime> lastUpdated;
    private Map<String, String> extra;

    public DocumentMetadata(){
        converted = Optional.empty();
        remote = Optional.empty();
        tags = Optional.empty();
        extra = new HashMap<>();
        createdOn = Optional.empty();
        lastUpdated = Optional.empty();
    }

    public static DocumentMetadata extract(File f) throws IOException, URISyntaxException {
        DocumentMetadata documentMetadata = new DocumentMetadata();
        documentMetadata.setSource(f.toPath());
        try(Stream<String> lines = Files.lines(documentMetadata.getSource())){
            boolean inMetadataBlock = false;
            for(String line : (Iterable<String>)lines::iterator){
                if(METADATA_START.equals(line.trim())){
                    inMetadataBlock = true;
                    continue;
                }
                if(METADATA_END.equals(line.trim())){
                    break;
                }
                if(inMetadataBlock && !StringUtils.isBlank(line)){
                    String[] parts = StringUtils.split(line, META_DATA_KEY_VALUE_SEP, 2);
                    if("remote".equals(parts[0].trim())){
                        documentMetadata.setRemote(Optional.of(new URI(parts[1].trim())));
                    } else if("tags".equals(parts[0].trim())){
                        String[] tags = Arrays
                                .stream(parts[1].trim().split(META_DATA_VALUE_LIST_SEP))
                                .map(String::trim)
                                .toArray(String[]::new);
                        documentMetadata.setTags(Optional.of(Arrays.asList(tags)));
                    } else if("createdOn".equals(parts[0].trim())){
                        documentMetadata.setCreatedOn(Optional.of(ZonedDateTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ"))));
                    } else if("lastUpdated".equals(parts[0].trim())){
                        documentMetadata.setLastUpdated(Optional.of(ZonedDateTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ"))));
                    } else if("title".equals(parts[0].trim())){
                        documentMetadata.setTitle(parts[1].trim());
                    } else{
                        documentMetadata.getExtra().put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }

        return documentMetadata;
    }

    public Path getSource() {
        return source;
    }

    public void setSource(Path source) {
        this.source = source;
    }

    public Optional<Path> getConverted() {
        return converted;
    }

    public void setConverted(Optional<Path> converted) {
        this.converted = converted;
    }

    public Optional<URI> getRemote() {
        return remote;
    }

    public void setRemote(Optional<URI> remote) {
        this.remote = remote;
    }

    public Optional<List<String>> getTags() {
        return tags;
    }

    public void setTags(Optional<List<String>> tags) {
        this.tags = tags;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }

    public Optional<ZonedDateTime> getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Optional<ZonedDateTime> createdOn) {
        this.createdOn = createdOn;
    }

    public Optional<ZonedDateTime> getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Optional<ZonedDateTime> lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
