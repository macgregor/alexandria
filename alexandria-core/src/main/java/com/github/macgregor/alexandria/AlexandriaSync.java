package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.Remote;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.util.Optional;

@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaSync {

    @NonNull private Context context;

    public void syncWithRemote() throws AlexandriaException {
        log.debug("Syncing files to html.");
        Remote remote = configureRemote(context);

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Syncing %s with remote.", metadata.sourcePath().toFile().getName()));
            remote.validateDocumentMetadata(metadata);

            // since context isnt stored between runs we lose the path to the converted html file between commands
            // e.g. between alexandria:convert and alexandria:sync goals. This is inefficient, may want to checksum
            // html files and store the value in config
            if(!context.convertedPath(metadata).isPresent() && !AlexandriaConvert.supportsNativeMarkdown(context)){
                AlexandriaConvert.convert(context, metadata);
                log.debug(String.format("Reconverted %s", metadata.sourcePath().toFile().getName()));
            }

            long currentChecksum = FileUtils.checksumCRC32(metadata.sourcePath().toFile());
            log.debug(String.format("Old checksum: %d; New checksum: %d", metadata.sourceChecksum().orElse(null), currentChecksum));
            if (!metadata.remoteUri().isPresent()) {
                remote.create(context, metadata);
                log.info(String.format("Created new document at %s", metadata.remoteUri().orElse(null)));
            } else {
                if (!metadata.sourceChecksum().isPresent() || !metadata.sourceChecksum().get().equals(currentChecksum)) {
                    remote.update(context, metadata);
                    log.info(String.format("Updated document %s at %s",
                            metadata.sourcePath().toFile().getName(), metadata.remoteUri().orElse(null)));
                } else{
                    log.info(String.format("%s is already up to date (checksum: %d, last updated: %s)",
                            metadata.sourcePath().toFile().getName(), currentChecksum, metadata.lastUpdated().orElse(null)));
                }
            }
            metadata.sourceChecksum(Optional.of(currentChecksum));
        }, (context, exceptions) -> {
            log.info(String.format("Synced %d out of %d documents with remote %s",
                    context.config().metadata().get().size() - exceptions.size(),
                    context.config().metadata().get().size(),
                    context.config().remote().baseUrl().orElse(null)));
            return false;
        });
    }

    protected Remote configureRemote(Context context) throws AlexandriaException {
        try {
            Class remoteClass = Class.forName(context.config().remote().clazz());
            Remote remote = (Remote)remoteClass.newInstance();
            remote.configure(context.config().remote());
            remote.validateRemoteConfig();
            return remote;
        } catch (Exception e) {
            log.warn(String.format("Unable to instantiate remote of type %s", context.config().remote().clazz()), e);
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unable to instantiate remote of type %s", context.config().remote().clazz()))
                    .causedBy(e)
                    .build();
        }
    }
}
