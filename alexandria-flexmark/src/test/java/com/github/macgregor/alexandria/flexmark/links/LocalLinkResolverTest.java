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

public class LocalLinkResolverTest {

    private LinkResolver alexandriaResolver = mock(LinkResolver.class);
    private LinkResolverContext linkResolverContext = mock(LinkResolverContext.class);
    private Link node = mock(Link.class);
    private DataHolder contextOptions = mock(DataHolder.class);
    private LocalLinkResolver.Factory relativeLinkResolverFactory = new LocalLinkResolver.Factory(alexandriaResolver);

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
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void testNewRelativeLinkResolveWithOptions() {
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(linkResolverContext, alexandriaResolver);
        assertThat(localLinkResolver.getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void tesRelativeLinkResolverFactoryCreatesRelativeLinkResolver() {
        com.vladsch.flexmark.html.LinkResolver relativeLinkResolver = relativeLinkResolverFactory.create(linkResolverContext);
        assertThat(relativeLinkResolver).isInstanceOf(LocalLinkResolver.class);

        assertThat(((LocalLinkResolver)relativeLinkResolver).getAlexandriaFlexmarkLinkResolver()).isEqualTo(alexandriaResolver);
    }

    @Test
    public void testRelativeLinkResolverFactoryDoesntAffectGlobalScope() {
        LocalLinkResolver.Factory relativeLinkResolverFactory = new LocalLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.affectsGlobalScope()).isFalse();
    }

    @Test
    public void testRelativeLinkResolverFactoryHasNoBeforeDependents() {
        LocalLinkResolver.Factory relativeLinkResolverFactory = new LocalLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.getBeforeDependents()).isNull();
    }

    @Test
    public void testRelativeLinkResolverFactoryHasNoAfterDependents() {
        LocalLinkResolver.Factory relativeLinkResolverFactory = new LocalLinkResolver.Factory(alexandriaResolver);
        assertThat(relativeLinkResolverFactory.getAfterDependents()).isNull();
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusValid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);

        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.VALID);
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusInvalid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(false);

        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.INVALID);
    }

    @Test
    public void testRelativeLinkResolverDelegatesLinkStatusException() {
        when(alexandriaResolver.isValid(any(), any())).thenThrow(new RuntimeException());

        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.delegateLinkStatus(node)).isEqualTo(LinkStatus.UNKNOWN);
    }

    @Test
    public void testRelativeLinkResolverDelegatesResolveLink() {
        ResolvedLink fallback = mock(ResolvedLink.class);

        ResolvedLink expected = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.VALID);
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.delegateResolveLink(node, fallback)).isEqualTo(expected);
    }

    @Test
    public void testRelativeLinkResolverDelegatesResolveLinkExceptionFallsbackToCurrentLink() throws Exception {
        ResolvedLink fallback = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        when(alexandriaResolver.resolve(any(), any())).thenThrow(new AlexandriaException());

        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        ResolvedLink actual = localLinkResolver.delegateResolveLink(node, fallback);
        assertThat(actual).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolvesOnlyRelativeLinkTypes() {
        ResolvedLink fallback = new ResolvedLink(new LinkType("Foo"), "https://www.google.com", null, LinkStatus.UNCHECKED);
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverValid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);

        ResolvedLink fallback = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        ResolvedLink expected = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.VALID);
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(expected);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverInvalid() {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(false);

        ResolvedLink fallback = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.INVALID);
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }

    @Test
    public void testRelativeLinkResolverResolveDelegatesStatusToAlexandriaLinkResolverException() throws Exception {
        when(alexandriaResolver.isValid(any(), any())).thenReturn(true);
        when(alexandriaResolver.resolve(any(), any())).thenThrow(new AlexandriaException());

        ResolvedLink fallback = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNCHECKED);
        ResolvedLink expected = new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com", null, LinkStatus.UNKNOWN);
        LocalLinkResolver localLinkResolver = new LocalLinkResolver(alexandriaResolver);
        assertThat(localLinkResolver.resolveLink(node, linkResolverContext, fallback)).isEqualTo(fallback);
    }
}
