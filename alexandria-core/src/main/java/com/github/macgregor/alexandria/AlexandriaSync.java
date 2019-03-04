package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.Remote;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.util.Optional;

/**
 * Sync indexed documents with the configured remote.
 *
 * For each document, the state of the file is determined and the appropriate {@link Remote}
 * method is called. Some metadata will be updated by this class, but most is delegated to
 * the remote implementation as it is dependent on information from the rest response.
 *
 * @see Remote
 * @see com.github.macgregor.alexandria.Config.DocumentMetadata#determineState()
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
     * @see Context#configureRemote()
     *
     * @param context  Alexandria context containing the remote config and indexed documents to sync
     * @throws AlexandriaException  Exception wrapping all exceptions thrown configuring the remote
     */
    public AlexandriaSync(Context context) throws AlexandriaException {
        this.context = context;
        this.remote = context.configureRemote();
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
     * {@link Context#save(Context)} will be called after each document is handled to ensure an unexpected problem
     * in the batch wont make the local state differ from the remote state. For example, creating a document and not saving
     * the {@code remoteUri} would cause Alexandria to create a new document on the remote on the next run.
     *
     * @see Remote
     * @see com.github.macgregor.alexandria.Config.DocumentMetadata#determineState()
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown while syncing documents
     */
    public void syncWithRemote() throws AlexandriaException {
        log.debug("Initiating sync with remote.");

        context.makePathsAbsolute();

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Syncing %s with remote.", metadata.sourceFileName()));
            remote.validateDocumentMetadata(metadata);

            long currentChecksum;
            Config.DocumentMetadata.State state = metadata.determineState();
            switch(state){
                case DELETE:
                    remote.delete(metadata);
                    log.info(String.format("%s (remote: %s) deleted from remote. Local file will not be removed by Alexandria.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case CREATE:
                    AlexandriaConvert.convertAsNeeded(context, metadata, remote.markdownConverter());
                    if(Resources.fileContentsAreBlank(metadata.sourcePath().toString())){
                        log.info(String.format("%s has no contents, not creating on remote", metadata.sourceFileName()));
                    } else{
                        remote.create(metadata);
                        currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
                        metadata.sourceChecksum(Optional.of(currentChecksum));
                        log.info(String.format("%s (remote: %s) created on remote", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    }
                    break;
                case UPDATE:
                    AlexandriaConvert.convertAsNeeded(context, metadata, remote.markdownConverter());
                    if(Resources.fileContentsAreBlank(metadata.sourcePath().toString())){
                        log.info(String.format("%s has no contents, not updating on remote", metadata.sourceFileName()));
                    } else {
                        remote.update(metadata);
                        currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
                        metadata.sourceChecksum(Optional.of(currentChecksum));
                        log.info(String.format("%s (remote: %s) updated on remote.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    }
                    break;
                case DELETED:
                case CURRENT:
                    log.info(String.format("%s (remote: %s) already current with remote: %s", metadata.sourceFileName(), metadata.remoteUri().orElse(null), state));
                    break;
            }
            Context.save(context);
        }, (context, exceptions) -> {
            log.info(String.format("Synced %d out of %d documents with remote %s",
                    context.documentCount() - exceptions.size(), context.documentCount(),
                    context.config().remote().baseUrl().orElse(null)));
            Context.save(context);
            return BatchProcess.EXCEPTIONS_UNHANDLED;
        });
    }
}
