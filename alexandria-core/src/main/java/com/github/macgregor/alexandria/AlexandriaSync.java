package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.Remote;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Sync indexed documents with the configured remote.
 *
 * For each document, the state of the file is determined and the appropriate {@link Remote}
 * method is called. Some metadata will be updated by this class, but most is delegated to
 * the remote implementation as it is dependent on information from the rest response.
 *
 * @see Remote
 * @see com.github.macgregor.alexandria.remotes.JiveRemote
 * @see com.github.macgregor.alexandria.Config.DocumentMetadata#determineState(Context)
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaSync {

    @NonNull private Context context;
    @NonNull private Remote remote;

    /**
     * Instantiate from the context and configure a remote given the {@link Config#remote}.
     *
     * @see AlexandriaSync#configureRemote(Context)
     *
     * @param context  Alexandria context containing the remote config and indexed documents to sync
     * @throws AlexandriaException  Exception wrapping all exceptions thrown configuring the remote
     */
    public AlexandriaSync(Context context) throws AlexandriaException {
        this.context = context;
        this.remote = configureRemote(context);
    }

    /**
     * Execute the sync process.
     *
     * For each document, the state of the file is determined and the appropriate {@link Remote}
     * method is called.
     * <ul>
     *     <li>DELETE: delete document from remote</li>
     *     <li>CREATE: convert if neeed, create document with remote, calculates and sets {@code sourceChecksum} on metadata</li>
     *     <li>UPDATE: convert if needed, create document with remote, calculates and sets {@code sourceChecksum} on metadata</li>
     *     <li>CURRENT: ignore</li>
     *     <li>DELETED: ignore</li>
     * </ul>
     *
     * {@link Alexandria#save(Context)} will be called after each document is handled to ensure an unexpected problem
     * in the batch wont make the local state differ from the remote state. For example, creating a document and not saving
     * the {@code remoteUri} would cause Alexandria to create a new document on the remote on the next run.
     *
     * @see Remote
     * @see com.github.macgregor.alexandria.remotes.JiveRemote
     * @see com.github.macgregor.alexandria.Config.DocumentMetadata#determineState(Context)
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown while syncing documents
     */
    public void syncWithRemote() throws AlexandriaException {
        log.debug("Initiating sync with remote.");

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Syncing %s with remote.", metadata.sourceFileName()));
            remote.validateDocumentMetadata(metadata);

            long currentChecksum;
            Config.DocumentMetadata.State state = metadata.determineState(context);
            switch(state){
                case DELETE:
                    remote.delete(context, metadata);
                    log.info(String.format("%s (remote: %s) deleted from remote. Local file will remain.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case CREATE:
                    convertAsNeeded(context, metadata);
                    remote.create(context, metadata);
                    currentChecksum = FileUtils.checksumCRC32(context.resolveRelativePath(metadata.sourcePath()).toFile());
                    metadata.sourceChecksum(Optional.of(currentChecksum));
                    log.info(String.format("%s (remote: %s) created on remote", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case UPDATE:
                    convertAsNeeded(context, metadata);
                    remote.update(context, metadata);
                    currentChecksum = FileUtils.checksumCRC32(context.resolveRelativePath(metadata.sourcePath()).toFile());
                    metadata.sourceChecksum(Optional.of(currentChecksum));
                    log.info(String.format("%s (remote: %s) updated on remote.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case DELETED:
                case CURRENT:
                    log.info(String.format("%s (remote: %s) already current with remote: %s", metadata.sourceFileName(), metadata.remoteUri().orElse(null), state));
                    break;
            }
            Alexandria.save(context);
        }, (context, exceptions) -> {
            log.info(String.format("Synced %d out of %d documents with remote %s",
                    context.documentCount() - exceptions.size(), context.documentCount(),
                    context.config().remote().baseUrl().orElse(null)));
            return BatchProcess.EXCEPTIONS_UNHANDLED;
        });
    }

    /**
     * Instantiate a {@link Remote} implementation based on the {@link Config#remote}.
     *
     * The class instantiation logic is very simple, but should be adequate for this simple use case. Essentially we just
     * pick the right class using the fully qualified class name in {@link com.github.macgregor.alexandria.Config.RemoteConfig#clazz}.
     * Implementation specific configuration and validation is delegated to the implementing class by calling
     * {@link Remote#configure(Config.RemoteConfig)} and {@link Remote#validateRemoteConfig()}.
     *
     * @see com.github.macgregor.alexandria.remotes.NoopRemote
     * @see com.github.macgregor.alexandria.remotes.JiveRemote
     *
     * @param context  Alexandria context containing the remote configuration
     * @return  configured remote ready for use
     * @throws AlexandriaException  Exception wrapping any exception thrown instantiation, configuring or validating the remote
     */
    protected Remote configureRemote(Context context) throws AlexandriaException {
        try {
            Class remoteClass = Class.forName(context.config().remote().clazz());
            Remote remote = (Remote) remoteClass.newInstance();
            remote.configure(context.config().remote());
            remote.validateRemoteConfig();
            return remote;
        } catch(Exception e){
            throw new AlexandriaException.Builder()
                    .withMessage("Unable to instantiate remote class " + context.config().remote().clazz())
                    .causedBy(e)
                    .build();
        }
    }

    /**
     * Determine if the document needs to be converted to html. Doesnt actually do the conversion or update {@link Context}.
     *
     * If the remote supports native markdown, we dont need to convert. Otherwise we get the converted file path from
     * {@link Context#convertedPath(Config.DocumentMetadata)} or calculate what it should be if the path isnt present.
     * Once we have a path, we calculate the current checksum of the converted document and compare it to
     * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#convertedChecksum} and only convert if it is different.
     *
     * @see AlexandriaConvert#convert(Context, Config.DocumentMetadata)
     *
     * TODO: move to {@link AlexandriaConvert}
     *
     * @param context  Alexandria context containing the converted path cache
     * @param metadata  Indexed document being synced
     * @return  False if remote supports native markdown or the converted file path exists and is up to date, otherwise true.
     * @throws IOException  Error calculating checksum
     */
    protected static boolean needsConversion(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(context.config().remote().supportsNativeMarkdown()){
            return false;
        }

        //look for converted file in the context cache or recalculate the path if its not there
        Path convertedPathGuess = context.convertedPath(metadata)
                .orElse(AlexandriaConvert.convertedPath(context, metadata));

        //if the file exists, check if it matches the stored checksum
        if(convertedPathGuess.toFile().exists()){
            long currentChecksum = FileUtils.checksumCRC32(convertedPathGuess.toFile());
            if(metadata.convertedChecksum().isPresent() && metadata.convertedChecksum().get().equals(currentChecksum)){
                return false;
            }
        }

        //remote needs conversion and the file doesnt exist
        return true;
    }

    /**
     * Converts an indexed document to html if needed.
     *
     * When Alexandria is run with its full lifecycle (index, convert, sync) this should be a noop, but if a user is running
     * individual phases we could find ourselves in a position where the files havent been converted yet, or they have been
     * converted but the file path isnt in the {@link Context#convertedPaths} cache. Instead of throwing an error, we
     * simply reconvert the file on the fly and add it to the context.
     *
     * If the remote supports native markdown, this is a noop.
     *
     * @see AlexandriaConvert#convert(Context, Config.DocumentMetadata)
     *
     * TODO: move to {@link AlexandriaConvert}
     *
     * @param context  Alexandria context containing the converted path cache
     * @param metadata  Indexed document being synced
     * @throws IOException  Errors calculating checksums or general file IO problems
     */
    protected static void convertAsNeeded(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(needsConversion(context, metadata)) {
            AlexandriaConvert.convert(context, metadata);
        }
    }
}
