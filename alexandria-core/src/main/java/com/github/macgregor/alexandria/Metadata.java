package com.github.macgregor.alexandria;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Metadata {

    public static final String METADATA_START = "<!--- alexandria";
    public static final String METADATA_END = "-->";
    public static final String META_DATA_KEY_VALUE_SEP = ":";
    public static final String META_DATA_VALUE_LIST_SEP = ",";

    private Path source;
    private Optional<Path> converted;
    private Optional<URL> remote;
    private Optional<List<String>> tags;

    public Metadata(){
        converted = Optional.empty();
        remote = Optional.empty();
        tags = Optional.empty();
    }

    public static Metadata extract(File f) throws IOException {
        Metadata metadata = new Metadata();
        metadata.setSource(f.toPath());

        Map<String,String> metadataMap = new HashMap<>();
        try(Stream<String> lines = Files.lines(metadata.getSource())){
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
                    metadataMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        if(metadataMap.containsKey("remote")) {
            metadata.setRemote(Optional.of(new URL(metadataMap.get("remote"))));
        }

        if(metadataMap.containsKey("tags")) {
            String[] tags = Arrays.stream(metadataMap.get("tags")
                    .split(META_DATA_VALUE_LIST_SEP))
                    .map(String::trim)
                    .toArray(String[]::new);
            metadata.setTags(Optional.of(Arrays.asList(tags)));
        }

        return metadata;
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

    public Optional<URL> getRemote() {
        return remote;
    }

    public void setRemote(Optional<URL> remote) {
        this.remote = remote;
    }

    public Optional<List<String>> getTags() {
        return tags;
    }

    public void setTags(Optional<List<String>> tags) {
        this.tags = tags;
    }
}
