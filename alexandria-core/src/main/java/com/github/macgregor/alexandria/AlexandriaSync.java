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

import static com.github.macgregor.alexandria.AlexandriaSync.State.DELETED;

@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaSync {

    @NonNull private Context context;
    @NonNull private Remote remote;

    public AlexandriaSync(Context context) throws AlexandriaException {
        this.context = context;
        this.remote = configureRemote(context);
    }

    public void syncWithRemote() throws AlexandriaException {
        log.debug("Syncing files to html.");

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Syncing %s with remote.", metadata.sourceFileName()));
            remote.validateDocumentMetadata(metadata);

            State state = determineState(context, metadata);
            switch(state){
                case DELETE:
                    remote.delete(context, metadata);
                    log.info(String.format("%s (remote: %s) deleted from remote. Local file will remain.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case CREATE:
                    convertAsNeeded(context, metadata);
                    remote.create(context, metadata);
                    log.info(String.format("%s (remote: %s) created on remote", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case UPDATE:
                    convertAsNeeded(context, metadata);
                    remote.update(context, metadata);
                    log.info(String.format("%s (remote: %s) updated on remote.", metadata.sourceFileName(), metadata.remoteUri().orElse(null)));
                    break;
                case DELETED:
                case CURRENT:
                    log.info(String.format("%s (remote: %s) already current with remote: %s", metadata.sourceFileName(), metadata.remoteUri().orElse(null), state));
                    break;
            }
            long currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
            metadata.sourceChecksum(Optional.of(currentChecksum));
        }, (context, exceptions) -> {
            log.info(String.format("Synced %d out of %d documents with remote %s",
                    context.documentCount() - exceptions.size(), context.documentCount(),
                    context.config().remote().baseUrl().orElse(null)));
            return BatchProcess.EXCEPTIONS_UNHANDLED;
        });
    }

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

    protected static State determineState(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(metadata.deletedOn().isPresent()){
            return DELETED;
        }

        if (!metadata.remoteUri().isPresent()) {
            return State.CREATE;
        }

        if(metadata.extraProps().isPresent() && metadata.extraProps().get().containsKey("delete")){
            return State.DELETE;
        }

        long currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
        if(metadata.sourceChecksum().isPresent() && metadata.sourceChecksum().get().equals(currentChecksum)){
            return State.CURRENT;
        }

        return State.UPDATE;
    }

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

    protected static void convertAsNeeded(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(needsConversion(context, metadata)) {
            AlexandriaConvert.convert(context, metadata);
        }
    }

    public enum State{
        CREATE, UPDATE, DELETE, DELETED, CURRENT;
    }
}
