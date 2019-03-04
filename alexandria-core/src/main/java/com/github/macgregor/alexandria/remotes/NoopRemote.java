package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;
import com.github.macgregor.alexandria.markdown.NoopMarkdownConverter;
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

    private MarkdownConverter markdownConverter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Config.RemoteConfig config) {
        log.debug("Noop - Configuring remote.");
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Creating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Updating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Deleting %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    @Override
    public MarkdownConverter markdownConverter() {
        log.debug("Noop - Creating Noop markdown converted.");
        return new NoopMarkdownConverter();
    }

    @Override
    public void markdownConverter(MarkdownConverter markdownConverter) {
        log.debug("Noop - Setting Markdown converter to {}.", markdownConverter.getClass().getSimpleName());
        this.markdownConverter = markdownConverter;
    }
}
