package com.github.macgregor.alexandria;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

public class AlexandriaConfig {
    public static final DateFormat DEFAULT_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private Optional<List<RemoteConfig>> remotes = Optional.of(new ArrayList<>());
    private Optional<DateFormat> datetimeFormat = Optional.of(DEFAULT_DATETIME);
    private Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());
    private Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

    public Optional<DateFormat> getDatetimeFormat() {
        return datetimeFormat;
    }

    public void setDatetimeFormat(Optional<DateFormat> datetimeFormat) {
        this.datetimeFormat = datetimeFormat;
    }

    public Optional<List<String>> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Optional<List<String>> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public Optional<List<RemoteConfig>> getRemotes() {
        return remotes;
    }

    public void setRemotes(Optional<List<RemoteConfig>> remotes) {
        this.remotes = remotes;
    }

    public Optional<List<DocumentMetadata>> getMetadata() {
        return metadata;
    }

    public void setMetadata(Optional<List<DocumentMetadata>> metadata) {
        this.metadata = metadata;
    }

    public static class RemoteConfig{
        private String baseUrl;
        private String username;
        private String password;
        private Optional<Boolean> nativeMarkdown = Optional.of(false);
        private Optional<Boolean> enabled = Optional.of(true);
        private Optional<DateFormat> datetimeFormat = Optional.of(DEFAULT_DATETIME);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Optional<Boolean> getNativeMarkdown() {
            return nativeMarkdown;
        }

        public void setNativeMarkdown(Optional<Boolean> nativeMarkdown) {
            this.nativeMarkdown = nativeMarkdown;
        }

        public Optional<Boolean> getEnabled() {
            return enabled;
        }

        public void setEnabled(Optional<Boolean> enabled) {
            this.enabled = enabled;
        }

        public Optional<DateFormat> getDatetimeFormat() {
            return datetimeFormat;
        }

        public void setDatetimeFormat(Optional<DateFormat> datetimeFormat) {
            this.datetimeFormat = datetimeFormat;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public class DocumentMetadata{
        private Path source;
        private String title;
        private Optional<Path> converted = Optional.empty();
        private Optional<URI> remote = Optional.empty();
        private Optional<List<String>> tags = Optional.of(new ArrayList<>());
        private Optional<ZonedDateTime> createdOn = Optional.empty();
        private Optional<ZonedDateTime> lastUpdated = Optional.empty();
        private Optional<Map<String, String>> extra = Optional.of(new HashMap<>());

        public Path getSource() {
            return source;
        }

        public void setSource(Path source) {
            this.source = source;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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

        public Optional<Map<String, String>> getExtra() {
            return extra;
        }

        public void setExtra(Optional<Map<String, String>> extra) {
            this.extra = extra;
        }
    }

    /**
     * Config parsing code
     */
    protected static JavaPropsMapper propertyMapper;
    protected static ObjectMapper yamlMapper;
    static{
        propertyMapper = new JavaPropsMapper();
        propertyMapper.setDateFormat(DEFAULT_DATETIME);
        yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setDateFormat(DEFAULT_DATETIME);

        Module jdk8Module = new Jdk8Module();
        propertyMapper.registerModule(jdk8Module);
        yamlMapper.registerModule(jdk8Module);
    }

    protected enum PropertiesFileType{
        YAML, PROP;
    }

    private PropertiesFileType type;
    private Path propertiesPath;

    public static AlexandriaConfig load(String filePath) throws IOException {
        Path propertiesPath = Resources.path(filePath, true);

        try {
            AlexandriaConfig config = propertyMapper.readValue(propertiesPath.toFile(), AlexandriaConfig.class);
            config.type = PropertiesFileType.PROP;
            config.propertiesPath = propertiesPath;
            return config;
        } catch (IOException first) {
            try {
                AlexandriaConfig config = yamlMapper.readValue(propertiesPath.toFile(), AlexandriaConfig.class);
                config.type = PropertiesFileType.YAML;
                config.propertiesPath = propertiesPath;
                return config;
            } catch (IOException second) {
                throw new IOException("Unable to load %s as either .properties or .yaml file", second);
            }
        }
    }

    public static void save(AlexandriaConfig config) throws IOException {
        if(PropertiesFileType.PROP.equals(config.type)) {
            propertyMapper.writeValue(config.propertiesPath.toFile(), config);
        } else if(PropertiesFileType.YAML.equals(config.type)) {
            yamlMapper.writeValue(config.propertiesPath.toFile(), config);
        }
    }
}
