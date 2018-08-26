package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Remote implementation that does nothing.
 * <p>
 * This is the default remote implementation that will be generated when no existing {@link Config} is found to work with.
 * It is also useful for testing before making large changes to the remote
 */
@Slf4j
@NoArgsConstructor
public class NoopRemote implements Remote {

    /**
     * {@inheritDoc}
     *
     * @param config
     */
    @Override
    public void configure(Config.RemoteConfig config) {
        log.debug("Noop - Configuring remote.");
        return;
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param metadata
     * @throws IOException
     */
    @Override
    public void create(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Creating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param metadata
     * @throws IOException
     */
    @Override
    public void update(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Updating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param metadata
     * @throws IOException
     */
    @Override
    public void delete(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Deleting %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }
}
