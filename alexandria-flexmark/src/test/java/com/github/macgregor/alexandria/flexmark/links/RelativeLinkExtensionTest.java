package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class RelativeLinkExtensionTest {

    private LinkResolver alexandriaFlexmarkLinkResolver = mock(LinkResolver.class);
    private HtmlRenderer.Builder rendererBuilder = new HtmlRenderer.Builder();
    private Parser.Builder parserBuilder = new Parser.Builder();
    private RelativeLinkExtension relativeLinkExtension = new RelativeLinkExtension(alexandriaFlexmarkLinkResolver);

    @Test
    public void testRelativeLinkResolverExtensionRendererExtensionHtmlRenderType(){
        rendererBuilder.set(HtmlRenderer.TYPE, "HTML");
        relativeLinkExtension.extend(rendererBuilder, "");
        assertThat(relativeLinkExtension.getRelativeLinkNodeRendererFactory()).isNotNull();
        assertThat(relativeLinkExtension.getRelativeLinkResolverFactory()).isNotNull();
    }

    @Test
    public void testRelativeLinkResolverExtensionRendererExtensionNonHtmlRenderType(){
        rendererBuilder.set(HtmlRenderer.TYPE, "FOO");
        relativeLinkExtension.extend(rendererBuilder, "");
        assertThat(relativeLinkExtension.getRelativeLinkNodeRendererFactory()).isNull();
        assertThat(relativeLinkExtension.getRelativeLinkResolverFactory()).isNull();
    }

    @Test
    public void testRelativeLinkResolverExtensionExtendParser(){
        relativeLinkExtension.extend(parserBuilder);
        assertThat(relativeLinkExtension.getRelativeLinkRefProcessorFactory()).isNotNull();

    }

    @Test
    public void testRelativeLinkResolverExtensionRenderOptionsNoop(){
        RelativeLinkExtension copy = new RelativeLinkExtension(alexandriaFlexmarkLinkResolver);
        relativeLinkExtension.rendererOptions(mock(MutableDataHolder.class));
        assertThat(relativeLinkExtension).isEqualTo(copy);
    }

    @Test
    public void testRelativeLinkResolverExtensionParserOptionsNoop(){
        RelativeLinkExtension copy = new RelativeLinkExtension(alexandriaFlexmarkLinkResolver);
        relativeLinkExtension.parserOptions(mock(MutableDataHolder.class));
        assertThat(relativeLinkExtension).isEqualTo(copy);
    }

}
