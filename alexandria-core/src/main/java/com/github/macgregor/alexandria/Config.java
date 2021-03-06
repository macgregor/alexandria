package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Alexandria configuration that should be persisted between runs.
 *
 * Runtime specific configuration (i.e. {@link Context} fields) will not be saved as they are considered
 * specific to the runtime environment. At runtime Paths contained here can be assumed to be absolute paths
 * however when serialzed all paths should be converted to relatative paths to {@link Context#configPath} so
 * that they are valid across any runtime execition. In otherwords, if Alexandria is being used in a git repository,
 * the config file on disk shouldnt contain any absolute paths or system specific data that will break if committed
 * and used by another contributor.
 *
 * @see Context
 * @see Context#load(String)
 * @see Context#save(Context)
 * @see Jackson
 */
@Data
@Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class Config {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String VARIABLE_INTERPOLATION_PATTERN = "\\${env\\.(.*)}";

    /** Configuration properties for {@link com.github.macgregor.alexandria.remotes.Remote}. Default: {@link com.github.macgregor.alexandria.remotes.NoopRemote}. */
    @JsonProperty
    protected RemoteConfig remote = new RemoteConfig();

    /** List of indexed documents and metadata to keep them in sync with remote. Default: empty list. */
    @JsonProperty
    protected Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

    /** default tags added to all documents. Default: empty list. */
    @JsonProperty
    protected Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());

    /**
     * Configuration properties for instantiating and configuring a {@link com.github.macgregor.alexandria.remotes.Remote}.
     */
    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor @AllArgsConstructor
    @ToString
    public static class RemoteConfig{
        /** Fully qualified class name of the {@link com.github.macgregor.alexandria.remotes.Remote} implementation to use. Default: "com.github.macgregor.alexandria.remotes.NoopRemote" */
        @JsonProperty("class")
        protected String clazz = "com.github.macgregor.alexandria.remotes.NoopRemote";

        /** Fully qualified class name of the {@link com.github.macgregor.alexandria.markdown.MarkdownConverter} implementation to use. Default: "com.github.macgregor.alexandria.markdown.NoopMarkdownConverter" */
        @JsonProperty
        protected String converterClazz = "com.github.macgregor.alexandria.markdown.NoopMarkdownConverter";

        /** Base url to use for rest api calls. Default: none. */
        @JsonProperty
        protected Optional<String> baseUrl = Optional.empty();

        /** Username to use for rest api calls. Default: none. */
        @JsonProperty
        protected Optional<String> username = Optional.empty();

        /** Password to use for rest api calls. Default: none. */
        @JsonProperty
        protected Optional<String> password = Optional.empty();

        /** Format used to convert datetime strings in rest responses from the remote. Default: {@value Config#ALEXANDRIA_DATETIME_PATTERN}  */
        @JsonProperty
        protected String datetimeFormat = ALEXANDRIA_DATETIME_PATTERN;

        /** Timeout in seconds for each rest call to the remote. Default: 30 seconds. */
        @JsonProperty
        protected Integer requestTimeout = 30;

        /** Defaults extra properties to add to metadata. Metadata set values take precedent */
        @JsonProperty
        protected Optional<Map<String, String>> defaultExtraProps = Optional.empty();

        /**
         * Returns the remote username, interpolating the variable if it follows the appropriate pattern (e.g. ${env.foo}).
         *
         * @see Resources#interpolate(String)
         *
         * @return  The set username or interpolated variable resolved to an environment variable.
         */
        public Optional<String> username(){
            return Optional.ofNullable(Resources.interpolate(username.orElse(null)));
        }

        /**
         * Returns the remote password, interpolating the variable if it follows the appropriate pattern (e.g. ${env.foo}).
         *
         * @see Resources#interpolate(String)
         *
         * @return  The set password or interpolated variable resolved to an environment variable.
         */
        public Optional<String> password(){
            return Optional.ofNullable(Resources.interpolate(password.orElse(null)));
        }
    }

    /**
     * Metadata used to index a document for tracking and syncing purposes.
     *
     * Alexandria uses this metadata to determine what state the document is in, e.g. does it need to be created on the remote,
     * has it already been deleted from the remote, etc. Remote implementations will also use it to track remote specific
     * metadata in {@link DocumentMetadata#extraProps}, for example a document id needed to interact with the document via rest.
     * Only the remote implementation will be aware of and use these special properties, core document processing will
     * only use declared fields.
     */
    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor
    @ToString @AllArgsConstructor
    public static class DocumentMetadata{
        /**
         * File path to the indexed document which may or may not exist.
         *
         * At runtime, this will be an absolute path, but when persisted it will be converted to a path relative to
         * {@link Context#configPath}.
         *
         * @see Context#makePathsAbsolute()
         * @see Context#makePathsRelative()
         * */
        @JsonProperty
        @NonNull protected Path sourcePath;

        /** Title of the document. The indexing phase will default this to the file name. */
        @JsonProperty
        @NonNull protected String title;

        /**
         * Remote address of the document if it already exists on the remote. Default: none. (this will trigger a create)
         *
         * If not set, triggers a create when the sync phase is executed. The default state is to create the document. If
         * it already exists before using Alexandria, be sure to manually add the remote uri so that the remote document
         * is updated instead of duplicated.
         *
         * Remotes may or may not use this URI exactly to interact with the remote. For example, Jive has different
         * addresses for documents you interact with in a browser than documents you interact with through rest, so it
         * has to convert this human URI to a computer one.
         * */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<URI> remoteUri = Optional.empty();

        /** Tags to add to the document. Default: empty list.*/
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<List<String>> tags = Optional.of(new ArrayList<>());

        /** Simple file checksum on the source file path used to trigger updates. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<Long> sourceChecksum = Optional.empty();

        /** Simple file checksum on the converted file path used to trigger conversion. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<Long> convertedChecksum = Optional.empty();

        /** Datetime when file was created on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<ZonedDateTime> createdOn = Optional.empty();

        /** Datetime when file was updated on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<ZonedDateTime> lastUpdated = Optional.empty();

        /** Datetime when file was deleted on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<ZonedDateTime> deletedOn = Optional.empty();

        /** Extra properties needed by the remote implementation. Default: empty map. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        protected Optional<Map<String, String>> extraProps = Optional.of(new HashMap<>());

        @EqualsAndHashCode.Exclude
        protected Optional<Path> convertedPath = Optional.empty();

        @EqualsAndHashCode.Exclude
        protected Optional<Path> intermediateConvertedPath = Optional.empty();

        /**
         * Convenience method for checking if a document has an optional property in {@link #extraProps}.
         *
         * @param key  to look for in {@link #extraProps}.
         * @return true if the property key exists, false if {@link #extraProps} doesnt contain the key or is an empty optional.
         */
        public boolean hasExtraProperty(String key){
            return extraProps.isPresent() && extraProps.get().containsKey(key);
        }

        /**
         * Convenience method safely set an extra property.
         *
         * @param key  key to set or update
         * @param value  new value
         */
        public void setExtraProperty(String key, String value){
            if(!extraProps.isPresent()){
                extraProps = Optional.of(new HashMap<>());
            }
            extraProps.get().put(key, value);
        }

        /**
         * Convenience method to safetly get an extra property value. Has safe behavior as {@link Map#get(Object)}.
         *
         * @param key  key to retrieve from map
         * @return  the value in the map if it exists, or null if the value doesnt exist or the optional is empty
         */
        public String getExtraProperty(String key){
            if(hasExtraProperty(key)){
                return extraProps.get().get(key);
            }
            return null;
        }

        /**
         * Convenience method for getting the name of the document from {@link #sourcePath}.
         *
         * @return  name of the source document
         */
        public String sourceFileName(){
            return sourcePath.toFile().getName();
        }

        /**
         * Determine the state of a document when processing the sync phase.
         *
         * {@link #sourcePath} should be made absolute before calling this or you risk the checksum failing.
         *
         * @see Context#makePathsAbsolute()
         *
         * @return  state that can be used to determine how to process the document
         * @throws IOException  when problems working with {@link #sourcePath} occur.
         */
        public State determineState() throws IOException {
            if(this.deletedOn().isPresent()){
                return State.DELETED;
            }

            if(!sourcePath.toFile().exists()){
                return State.DELETE;
            }

            if (!this.remoteUri().isPresent()) {
                return State.CREATE;
            }

            long currentSourceChecksum = FileUtils.checksumCRC32(sourcePath.toFile());
            if(this.sourceChecksum().isPresent() && !this.sourceChecksum().get().equals(currentSourceChecksum)){
                return State.UPDATE;
            }

            // converters can modify rendered output meaning the output can change even when the source doesnt,
            // so we need to check that the previous and current checksums match to know if we need to update or not
            if(convertedPath().isPresent() && convertedChecksum().isPresent()){
                long currentConvertedChecksum = FileUtils.checksumCRC32(convertedPath().get().toFile());
                if(!this.convertedChecksum().get().equals(currentConvertedChecksum)){
                    return State.UPDATE;
                }
            }

            return State.CURRENT;
        }

        /**
         * Simple enum to provide type checked states for document processing.
         */
        public enum State{
            CREATE, UPDATE, DELETE, DELETED, CURRENT
        }
    }
}
