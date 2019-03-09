package com.github.macgregor.alexandria.flexmark;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.flexmark.links.LocalLinkExtension;
import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.util.sequence.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves relative links to their remote URI's if they exist.
 *
 * If a remote URI is not set yet (i.e. it hasnt been created yet) it will leave the
 * relative URL in the final href tag. Web urls are also left alone.
 */
@Slf4j
public class AlexandriaRemoteUriLinkResolver implements LinkResolver, Context.ContextAware {

    public static final String GITHUB_BASIC_LINK_REGEX = "(\\[)(.*?)(\\])(\\()(.+?)(\\))";
    public static final Pattern GITHUB_BASIC_LINK_PATTERN = Pattern.compile(GITHUB_BASIC_LINK_REGEX);

    private Context context;

    @Override
    public boolean wants(String rawLink) {
        if(StringUtils.isBlank(rawLink)){
            return false;
        }

        Matcher matcher = GITHUB_BASIC_LINK_PATTERN.matcher(rawLink);
        if(matcher.matches()){
            Range r = FlexmarkUtils.range(rawLink, matcher.group(5));
            String link = (String) rawLink.subSequence(r.getStart(), r.getEnd());
            if(FlexmarkUtils.isUrl(link)){
                return false;
            }
            if(FlexmarkUtils.isAbsolute(link)){
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * A valid relative link is:
     *      - a valid file system path
     *      - a file that exists
     *      - is not a directory
     *      - exists in the {@link Config#metadata()}
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
     * Resolves a relative file system link to a {@link Config.DocumentMetadata#remoteUri()}
     * if possible, otherwise the link is left alone.
     *
     * Assumes that {@link AlexandriaRemoteUriLinkResolver#isValid(String, String)} has been called and returns true.
     *
     * @param linkText  the text part of the parsed link e.g. "some text" in {@code [some text](./foo.md)}
     * @param link  the URL part of a parsed link, e.g. "./foo.md" in {@code [some text](./foo.md)}
     * @return  URI of the {@link Config.DocumentMetadata#remoteUri()} or the original link
     * @throws AlexandriaException {@link Context} hasnt been set, or the {@link Config.DocumentMetadata#remoteUri()}
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

