package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * Sets up the Flexmark extension to parse and resolve relative links.
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class LocalLinkExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension, Context.ContextAware {

    public static final LinkType RELATIVE_LINK = new LinkType("RELATIVE");

    private LinkResolver alexandriaFlexmarkLinkResolver;
    private Context context;
    private LocalLinkNodeRenderer.Factory relativeLinkNodeRendererFactory;
    private LocalLinkResolver.Factory relativeLinkResolverFactory;
    private LocalLinkRefProcessor.Factory relativeLinkRefProcessorFactory;

    public LocalLinkExtension(LinkResolver alexandriaFlexmarkLinkResolver){
        this.alexandriaFlexmarkLinkResolver = alexandriaFlexmarkLinkResolver;
    }

    @Override
    public void rendererOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        if (rendererBuilder.isRendererType("HTML")) {
            relativeLinkNodeRendererFactory = new LocalLinkNodeRenderer.Factory();
            relativeLinkResolverFactory = new LocalLinkResolver.Factory(alexandriaFlexmarkLinkResolver);
            rendererBuilder.nodeRendererFactory(relativeLinkNodeRendererFactory);
            rendererBuilder.linkResolverFactory(relativeLinkResolverFactory);
        }
    }

    @Override
    public void parserOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        relativeLinkRefProcessorFactory = new LocalLinkRefProcessor.Factory(alexandriaFlexmarkLinkResolver);
        parserBuilder.linkRefProcessorFactory(relativeLinkRefProcessorFactory);
    }

    @Override
    public void alexandriaContext(Context context) {
        this.context = context;
        if(alexandriaFlexmarkLinkResolver instanceof Context.ContextAware){
            ((Context.ContextAware)alexandriaFlexmarkLinkResolver).alexandriaContext(context);
        }
    }

    @Override
    public Context alexandriaContext() {
        return this.context;
    }

    public static LocalLinkExtension create(LinkResolver linkResolver){
        return new LocalLinkExtension(linkResolver);
    }
}
