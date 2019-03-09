package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;

import java.net.URI;

/**
 * Interface {@link MarkdownConverter} implementations can use to implement link resolving. This
 * should happen after parsing a link our of the markdown source and before rendering the HTML (or
 * whatever format the remote uses).
 *
 * For example this can be used to resolve relative links to markdown documents within a repo
 * to their remote URI. So {@code [some document](./some_document.md)} has a remote URI of
 * {@code https://www.jive.com/DOC-1234}, a properly implemented {@link LinkResolver} would
 * replace {@code ./some_document.md} with {@code https://www.jive.com/DOC-1234} in the rendered
 * HTML.
 *
 * See alexandria-flexmark and alexandria-remote-jive for examples.
 */
public interface LinkResolver {

    boolean wants(String rawLink);

    /**
     * Called before a call to {@link LinkResolver#resolve(String, String)} to ensure the resolver
     * can handle the link.
     *
     * @param linkText  the text part of the parsed link e.g. "some text" in {@code [some text](./foo.md)}
     * @param link  the URL part of a parsed link, e.g. "./foo.md" in {@code [some text](./foo.md)}
     * @return true if the {@link LinkResolver} finds the link acceptable, false otherwise
     */
    boolean isValid(String linkText, String link);

    /**
     * Resolves a parsed URL into the URL it should be in the final rendered document.
     *
     * It is not guaranteed that the link wont be altered by other parts of the {@link MarkdownConverter}
     * or whatever conversion library it uses (i.e. Flexmark). No current way to indicate to {@link MarkdownConverter}
     * that link processing should halt after resolving successfully.
     *
     * @param linkText  the text part of the parsed link e.g. "some text" in {@code [some text](./foo.md)}
     * @param link  the URL part of a parsed link, e.g. "./foo.md" in {@code [some text](./foo.md)}
     * @return  URI that will be used in the final rendered document.
     * @throws AlexandriaException  there was a problem resolving the link
     */
    URI resolve(String linkText, String link) throws AlexandriaException;
}
