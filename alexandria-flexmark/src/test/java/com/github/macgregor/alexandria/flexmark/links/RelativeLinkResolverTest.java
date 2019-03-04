package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.LinkStatus;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelativeLinkResolverTest {

    private LinkResolver alexandriaResolver = mock(LinkResolver.class);
    private LinkResolverContext linkResolverContext = mock(LinkResolverContext.class);
    private Link node = mock(Link.class);
    private RelativeLinkOptions relativeLinkOptions = new RelativeLinkOptions();
    private DataHolder contextOptions = mock(DataHolder.class);
    private RelativeLinkResolver.Factory relativeLinkResolverFactory = new RelativeLinkResolver.Factory(alexandriaResolver);

    @Before
    public void setup() throws Exception{
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);
        when(alexandriaResolver.resolve(any(), any())).thenReturn(new URI("https://www.google.com"));
        when(node.getText()).thenReturn(BasedSequenceImpl.of("link text"));
        when(node.getUrl()).thenReturn(BasedSequenceImpl.of("./foo/bar.txt"));
        when(contextOptions.get(any())).thenReturn(false);
        when(linkResolverContext.getOptions()).thenReturn(contextOptions);
    }


    @Test
    public void testNewRelativeLinkResolverDefaultOptions() {
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.getOptions()).isNotNull();
        assertThat(relativeLinkResolver.getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void testNewRelativeLinkResolveWithOptions() {
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(linkResolverContext, alexandriaResolver);
        assertThat(relativeLinkResolver.getOptions()).isEqualTo(relativeLinkOptions);
        assertThat(relativeLinkResolver.getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void tesRelativeLinkResolverFactoryCreatesRelativeLinkResolver() {
        com.vladsch.flexmark.html.LinkResolver relativeLinkResolver = relativeLinkResolverFactory.create(linkResolverContext);
        assertThat(relativeLinkResolver).isInstanceOf(RelativeLinkResolver.class);

        assertThat(((RelativeLinkResolver)relativeLinkResolver).getOptions()).isEqualTo(relativeLinkOptions);
        assertThat(((RelativeLinkResolver)relativeLinkResolver).getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void testRelativeLinkResolverFactoryDoesntAffectGlobalScope() {
        RelativeLinkResolver.Factory relativeLinkResolverFactory = new RelativeLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.affectsGlobalScope()).isFalse();
    }

    @Test
    public void testRelativeLinkResolverFactoryHasNoBeforeDependents() {
        RelativeLinkResolver.Factory relativeLinkResolverFactory = new RelativeLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.getBeforeDependents()).isNull();
    }

    @Test
    public void testRelativeLinkResolverFactoryHasNoAfterDependents() {
        RelativeLinkResolver.Factory relativeLinkResolverFactory = new RelativeLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.getAfterDependents()).isNull();
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusValid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);

        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.VALID);
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusInvalid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(false);

        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.INVALID);
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusException() {
        when(alexandriaResolver.isValid(any(), any())).thenThrow(new RuntimeException());

        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.UNKNOWN);
    }

    @Test
    public void testRelativeLinkResolverDelegatesResolveLink() {
        ResolvedLink fallback = mock(ResolvedLink.class);

        ResolvedLink expected = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.VALID);
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.delegateResolveLink(node, fallback)).isEqualTo(expected);
    }

    @Test
    public void testRelativeLinkResolverDelegatesResolveLinkExceptionFallsbackToCurrentLink() throws Exception {
        ResolvedLink fallback = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        when(alexandriaResolver.resolve(any(), any())).thenThrow(new AlexandriaException());

        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        ResolvedLink actual = relativeLinkResolver.delegateResolveLink(node, fallback);
        assertThat(actual).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolvesOnlyRelativeLinkTypes() {
        ResolvedLink fallback = new ResolvedLink(new LinkType("Foo"), "https://www.google.com", null, LinkStatus.UNCHECKED);
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverValid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);

        ResolvedLink fallback = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        ResolvedLink expected = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.VALID);
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(expected);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverInvalid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(false);

        ResolvedLink fallback = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.INVALID);
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverException() throws Exception {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);
        when(alexandriaResolver.resolve(any(), any())).thenThrow(new AlexandriaException());

        ResolvedLink fallback = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        ResolvedLink expected = new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNKNOWN);
        RelativeLinkResolver relativeLinkResolver = new RelativeLinkResolver(alexandriaResolver);
        assertThat(relativeLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }
}
