package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves relative links to their remote URI's if they exist.
 *
 * If a remote URI is not set yet (i.e. it hasnt been created yet) it will leave the
 * relative URL in the final href tag. Web urls are also left alone.
 */
@Slf4j
public class JivaRelativeLinkResolver implements LinkResolver, Context.ContextAware {

    private Context context;

    /**
     * A valid relative link is:
     *      - a valid file system path
     *      - a file that exists
     *      - is not a directory
     *      - exists in the {@link Config#metadata}
     *
     * This also means it needs the Alexandria {@link Context} to determine any of this. If called before
     * {@link Context} is set, it will return false.
     *
     * @param linkText  the text part of the parsed link e.g. "some text" in {@code [some text](./foo.md)}
     * @param link  the URL part of a parsed link, e.g. "./foo.md" in {@code [some text](./foo.md)}
     * @return
     */
    @Override
    public boolean isValid(String linkText, String link) {
        if(context == null){
            log.warn("Tried to check {} relative link validity before setting Alexandria context. Returning false.", link);
            return false;
        }

        Path p = context.absolutePath(Paths.get(link));
        if(p.toFile().exists() && p.toFile().isFile()){
            return context.isIndexed(p).isPresent();
        }
        return false;
    }

    /**
     * Resolves a relative file system link to a {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * if possible, otherwise the link is left alone.
     *
     * Assumes that {@link JivaRelativeLinkResolver#isValid(String, String)} has been called and returns true.
     *
     * @param linkText  the text part of the parsed link e.g. "some text" in {@code [some text](./foo.md)}
     * @param link  the URL part of a parsed link, e.g. "./foo.md" in {@code [some text](./foo.md)}
     * @return  URI of the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri} or the original link
     * @throws AlexandriaException {@link Context} hasnt been set, or the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     *      is malformed
     */
    @Override
    public URI resolve(String linkText, String link) throws AlexandriaException {
        if(context == null){
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Tried to resolve {} before Alexandria context set.", link))
                    .build();
        }

        Path p = context.absolutePath(Paths.get(link));
        Config.DocumentMetadata metadata = context.isIndexed(p).get();
        if(metadata.remoteUri().isPresent()){
            return metadata.remoteUri().get();
        } else{
            log.debug("Metadata for {} ({}) present, but no remote URI. Will keep local file link until document has been created on the remote.", link, p);
            try {
                return new URI(link);
            } catch (URISyntaxException e) {
                throw new AlexandriaException.Builder()
                        .withMessage(String.format("Cannot create URI for relative link {} ({})", link, p))
                        .metadataContext(metadata)
                        .causedBy(e)
                        .build();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void alexandriaContext(Context context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context alexandriaContext() {
        return context;
    }
}
