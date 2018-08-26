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
 * <p>
 * Runtime specific configuration (i.e. {@link Context} fields) will not be saved as they are considered
 * specific to the runtime environment. The data here should be normalized such that it is valid across from any
 * runtime execution. In otherwords, if Alexandria is being used in a git repository, the config file shouldnt
 * contain any absolute paths or system specific data that will break if committed and used by another contributor.
 * <p>
 * @see Context
 * @see Alexandria#load(String)
 * @see Alexandria#save(Context)
 * @see Jackson
 */
@Data
@Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class Config {
    public static final String ALEXANDRIA_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /** Configuration properties for {@link com.github.macgregor.alexandria.remotes.Remote}. Default: {@link com.github.macgregor.alexandria.remotes.NoopRemote}. */
    @JsonProperty
    private RemoteConfig remote = new RemoteConfig();

    /** List of indexed documents and metadata to keep them in sync with remote. Default: empty list. */
    @JsonProperty
    private Optional<List<DocumentMetadata>> metadata = Optional.of(new ArrayList<>());

    /** default tags added to all documents. Default: empty list. (TODO: not implemented yet). */
    @JsonProperty
    private Optional<List<String>> defaultTags = Optional.of(new ArrayList<>());

    /**
     * Configuration properties for instantiating and configuring a {@link com.github.macgregor.alexandria.remotes.Remote}.
     */
    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor @AllArgsConstructor
    @ToString
    public static class RemoteConfig{
        /** Fully qualified class name of remote implementation to use. Default: "com.github.macgregor.alexandria.remotes.NoopRemote" */
        @JsonProperty("class")
        private String clazz = "com.github.macgregor.alexandria.remotes.NoopRemote";

        /** Base url to use for rest api calls. Default: none. */
        @JsonProperty
        private Optional<String> baseUrl = Optional.empty();

        /** Username to use for rest api calls. Default: none. */
        @JsonProperty
        private Optional<String> username = Optional.empty();

        /** Password to use for rest api calls. Default: none. */
        @JsonProperty
        private Optional<String> password = Optional.empty();

        /** Whether the remote supports markdown documents natively, skipping the conversion phase. Default: false. */
        @JsonProperty
        private Boolean supportsNativeMarkdown = false;

        /** Format used to convert datetime strings in rest responses from the remote. Default: {@value Config#ALEXANDRIA_DATETIME_PATTERN}  */
        @JsonProperty
        private String datetimeFormat = ALEXANDRIA_DATETIME_PATTERN;

        /** Timeout in seconds for each rest call to the remote. Default: 60 seconds. */
        @JsonProperty
        private Integer requestTimeout = 60;
    }

    /**
     * Metadata used to index a document for tracking and syncing purposes.
     * <p>
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
         * File path to the indexed document relative to {@link Context#projectBase}.
         * <p>
         * You can get an absolute path at runtime by calling {@link Context#resolveRelativePath(Path)}.
         * */
        @JsonProperty
        @NonNull private Path sourcePath;

        /** Title of the document. The indexing phase will default this to the file name. */
        @JsonProperty
        @NonNull private String title;

        /**
         * Remote address of the document if it already exists on the remote. Default: none. (this will trigger a create)
         * <p>
         * If not set, triggers a create when the sync phase is executed. The default state is to create the document. If
         * it already exists before using Alexandria, be sure to manually add the remote uri so that the remote document
         * is updated instead of duplicated.
         * <p>
         * Remotes may or may not use this URI exactly to interact with the remote. For example, Jive has different
         * addresses for documents you interact with in a browser than documents you interact with through rest, so it
         * has to convert this human URI to a computer one. The URI the {@link com.github.macgregor.alexandria.remotes.JiveRemote}
         * cares about is stored in {@link #extraProps}.
         * */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<URI> remoteUri = Optional.empty();

        /** Tags to add to the document. Default: empty list.*/
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<List<String>> tags = Optional.of(new ArrayList<>());

        /** Simple file checksum on the source file path used to trigger updates. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Long> sourceChecksum = Optional.empty();

        /** Simple file checksum on the converted file path used to trigger conversion. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Long> convertedChecksum = Optional.empty();

        /** Datetime when file was created on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> createdOn = Optional.empty();

        /** Datetime when file was updated on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> lastUpdated = Optional.empty();

        /** Datetime when file was deleted on the remote. Remote implementation is responsible for managing. Default: none. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<ZonedDateTime> deletedOn = Optional.empty();

        /** Extra properties needed by the remote implementation. Default: empty map. */
        @JsonProperty
        @EqualsAndHashCode.Exclude
        private Optional<Map<String, String>> extraProps = Optional.of(new HashMap<>());

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
         * Convenience method for getting the name of the document from {@link #sourcePath}.
         *
         * @return
         */
        public String sourceFileName(){
            return sourcePath.toFile().getName();
        }

        /**
         * Determine the state of a document when processing the sync phase.
         * TODO: move to {@link Context}
         *
         * @param context  Alexandria context needed to properly resolve the relative {@link #sourcePath}.
         * @return
         * @throws IOException  when problems working with {@link #sourcePath} occur.
         */
        public State determineState(Context context) throws IOException {
            if(this.deletedOn().isPresent()){
                return State.DELETED;
            }

            if (!this.remoteUri().isPresent()) {
                return State.CREATE;
            }

            if(this.extraProps().isPresent() && this.extraProps().get().containsKey("delete")){
                return State.DELETE;
            }

            long currentChecksum = FileUtils.checksumCRC32(context.resolveRelativePath(this.sourcePath()).toFile());
            if(this.sourceChecksum().isPresent() && this.sourceChecksum().get().equals(currentChecksum)){
                return State.CURRENT;
            }

            return State.UPDATE;
        }

        /**
         * Simple enum to provide type checked states for document processing.
         */
        public enum State{
            CREATE, UPDATE, DELETE, DELETED, CURRENT
        }
    }
}
