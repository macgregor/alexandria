package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.exceptions.UncheckedAlexandriaException;
import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.parser.LinkRefProcessor;
import com.vladsch.flexmark.parser.LinkRefProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.regex.Matcher;

/**
 * Checks whether a link is eligible for the {@link LocalLinkResolver} to process.
 */
@Getter @NoArgsConstructor
public class LocalLinkRefProcessor implements LinkRefProcessor {

    private LinkResolver linkResolver;

    public LocalLinkRefProcessor(LinkResolver linkResolver) {
        this.linkResolver = linkResolver;
    }

    @Override
    public boolean getWantExclamationPrefix() {
        return false;
    }

    @Override
    public int getBracketNestingLevel() {
        return 0;
    }

    @Override
    public boolean isMatch(BasedSequence nodeChars) {
        return linkResolver.wants(nodeChars.toString());
    }

    @Override
    public Node createNode(BasedSequence nodeChars) {
        return new Link(nodeChars);
    }

    @Override
    public BasedSequence adjustInlineText(Document document, Node node) {
        if(!(node instanceof Link)){
            throw new UncheckedAlexandriaException("Flexmark error: Only RelativeLinkNodes should be passed here.");
        }

        final Link linkNode = (Link) node;
        return linkNode.getText().ifNull(linkNode.getUrl());
    }

    @Override
    public boolean allowDelimiters(BasedSequence chars, Document document, Node node) {
        return false;
    }

    @Override
    public void updateNodeElements(Document document, Node node) {
        return;
    }

    public static class Factory implements LinkRefProcessorFactory {
        private LinkResolver linkResolver;

        public Factory(LinkResolver linkResolver){
            this.linkResolver = linkResolver;
        }

        @Override
        public LinkRefProcessor create(Document document) {
            return new LocalLinkRefProcessor(linkResolver);
        }

        @Override
        public boolean getWantExclamationPrefix(DataHolder options) {
            return false;
        }

        @Override
        public int getBracketNestingLevel(DataHolder options) {
            return 0;
        }
    }
}

