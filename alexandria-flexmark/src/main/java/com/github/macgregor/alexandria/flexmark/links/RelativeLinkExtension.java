package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Sets up the Flexmark extension to parse and resolve relative links.
 */
@Getter
@EqualsAndHashCode
public class RelativeLinkExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
    public static final DataKey<Boolean> DISABLE_RENDERING = new DataKey<>("DISABLE_RENDERING", false);

    public static final String GITHUB_BASIC_LINK_REGEX = "(\\[)(.*?)(\\])(\\()(.+?)(\\))";
    public static final Pattern GITHUB_BASIC_LINK_PATTERN = Pattern.compile(GITHUB_BASIC_LINK_REGEX);

    public static final LinkType RELATIVE_LINK = new LinkType("RELATIVE");

    private LinkResolver alexandriaFlexmarkLinkResolver;
    private RelativeLinkNodeRenderer.Factory relativeLinkNodeRendererFactory;
    private RelativeLinkResolver.Factory relativeLinkResolverFactory;
    private RelativeLinkRefProcessor.Factory relativeLinkRefProcessorFactory;


    public RelativeLinkExtension(LinkResolver alexandriaFlexmarkLinkResolver){
        this.alexandriaFlexmarkLinkResolver = alexandriaFlexmarkLinkResolver;
    }

    @Override
    public void rendererOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        if (rendererBuilder.isRendererType("HTML")) {
            relativeLinkNodeRendererFactory = new RelativeLinkNodeRenderer.Factory();
            relativeLinkResolverFactory = new RelativeLinkResolver.Factory(alexandriaFlexmarkLinkResolver);
            rendererBuilder.nodeRendererFactory(relativeLinkNodeRendererFactory);
            rendererBuilder.linkResolverFactory(relativeLinkResolverFactory);
        }
    }

    @Override
    public void parserOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        relativeLinkRefProcessorFactory = new RelativeLinkRefProcessor.Factory();
        parserBuilder.linkRefProcessorFactory(relativeLinkRefProcessorFactory);
    }
}
