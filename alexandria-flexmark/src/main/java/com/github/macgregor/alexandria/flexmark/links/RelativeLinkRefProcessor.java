package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.exceptions.UncheckedAlexandriaException;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.parser.LinkRefProcessor;
import com.vladsch.flexmark.parser.LinkRefProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.regex.Matcher;

/**
 * Checks whether a link is eligible for the {@link RelativeLinkResolver} to process.
 */
@Getter
public class RelativeLinkRefProcessor implements LinkRefProcessor {

    private final RelativeLinkOptions options;

    public RelativeLinkRefProcessor(){
        options = new RelativeLinkOptions();
    }

    public RelativeLinkRefProcessor(Document document) {
        this.options = new RelativeLinkOptions(document);
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
        Matcher matcher = RelativeLinkExtension.GITHUB_BASIC_LINK_PATTERN.matcher(nodeChars);
        if(matcher.matches()){
            BasedSequence link = nodeChars.subSequence(range(nodeChars, matcher.group(5)));
            if(isUrl(link)){
                return false;
            }
            if(isAbsolute(link)){
                return false;
            }
            return true;
        }
        return false;
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

    protected boolean isUrl(BasedSequence link){
        try {
            URL url = new URL(link.toString());
            return true;
        } catch (MalformedURLException e) {}
        return false;
    }

    protected boolean isAbsolute(BasedSequence link){
        try{
            return Paths.get(link.toString()).isAbsolute();
        } catch (InvalidPathException | NullPointerException ex) {
            return true;
        }
    }

    protected static Range range(BasedSequence chars, CharSequence subSequence){
        int start = chars.indexOf(subSequence);
        int end = start + subSequence.length();
        return new Range(start, end);
    }

    public static class Factory implements LinkRefProcessorFactory {
        @Override
        public LinkRefProcessor create(Document document) {
            return new RelativeLinkRefProcessor(document);
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

