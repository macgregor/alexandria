package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.Remote;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.util.Arrays;
import java.util.List;
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
     * Execute the sync process with 2 passes allowing rendering that relies on state outside of the source document,
     * like resolving links for documents that dont exist yet, to work without the user invoking the command twice.
     *
     * This is a little bit of a hack to avoid complicating the sync process with a more elaborate ordering/state resolution
     * process that would likely involve more work on the {@link Remote} implementation which could get messy quickly.
     * Simply doing two passes should solve most cases and shouldn't be a huge performance issue. If large numbers of documents
     * are a problem, implementing parallel rest requests would be a better optimization to implement first.
     *
     * @throws AlexandriaException
     */
    public void syncWithRemote() throws AlexandriaException {
        log.debug("Initiating sync with remote.");

        context.makePathsAbsolute();

        log.info("Syncing with {}", context.config().remote().baseUrl().get());
        this.sync();

        if(remote.twoPassSync()) {
            log.info("Syncing with {} - Pass 2 (requested by {})",
                    context.config().remote().baseUrl().get(), remote.getClass().getSimpleName());
            this.sync();
        }
    }

    /**
     * Execute the sync process.
     *
     * For each document, the state of the file is determined and the appropriate {@link Remote}
     * method is called.
     * <ul>
     *     <li>DELETE: delete document from remote</li>
     *     <li>CREATE: create document with remote, calculates and sets {@code sourceChecksum} on metadata</li>
     *     <li>UPDATE: create document with remote, calculates and sets {@code sourceChecksum} on metadata</li>
     *     <li>CURRENT: ignore</li>
     *     <li>DELETED: ignore</li>
     * </ul>
     *
     * {@link Context#save(Context)} will be called after each document is handled to ensure an unexpected problem
     * in the batch wont make the local state differ from the remote state. For example, creating a document and not saving
     * the {@code remoteUri} would cause Alexandria to create a new document on the remote on the next run.
     *
     * Because {@link Remote} implementations can have behavior that results in different converted documents even if the
     * source has not changed (e.g. resolving remote links will render the remote uri only after the document they reference
     * is created), all documents are reconverted before determining their state. This may seem wasteful, especially if all
     * three Alexandria phases are run at once, it reduces algorithm complexity. If performance becomes a problem, consider
     * skipping {@link AlexandriaConvert#convert()} when run along with {@link AlexandriaSync#syncWithRemote()}.
     *
     * @see Remote
     * @see com.github.macgregor.alexandria.Config.DocumentMetadata#determineState()
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown while syncing documents
     */
    protected void sync() throws AlexandriaException {
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Syncing %s with remote.", metadata.sourceFileName()));
            remote.validateDocumentMetadata(metadata);

            List<Config.DocumentMetadata.State> unconvertableStates =
                    Arrays.asList(Config.DocumentMetadata.State.DELETED, Config.DocumentMetadata.State.DELETE);
            if(!unconvertableStates.contains(metadata.determineState())) {
                // always convert to catch when AlexandriaSync is run without AlexandriaConvert
                // this also catches things like markdown converters output changing when the source stays the same,
                // like resolving relative links to newly created remote URIs
                AlexandriaConvert.convert(context, metadata, remote.markdownConverter());
            }

            long currentChecksum;
            Config.DocumentMetadata.State state = metadata.determineState();
            switch(state){
                case DELETE:
                    remote.delete(metadata);
                    log.info(String.format("%s (remote: %s) deleted from remote. Local file will not be removed by Alexandria.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case CREATE:
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
