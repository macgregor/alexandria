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
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.Range;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.InvalidPathException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalLinkRefProcessorTest {

    private LinkResolver alexandriaFlexmarkLinkResolver = mock(LinkResolver.class);

    @Before
    public void setup(){
        when(alexandriaFlexmarkLinkResolver.wants(any())).thenReturn(true);
    }

    @Test
    public void testRelativeLinkProcessorDoesntWantNesterBrackets(){
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThat(localLinkRefProcessor.getBracketNestingLevel()).isEqualTo(0);
    }

    @Test
    public void testRelativeLinkProcessorDoesntWantExclaimation(){
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThat(localLinkRefProcessor.getWantExclamationPrefix()).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntAllowDelimiters(){
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThat(localLinkRefProcessor.allowDelimiters(mock(BasedSequence.class), mock(Document.class), mock(Node.class)))
                .isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntUpdateNodeElements(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        node.getUrl();
        Link copy = new Link(chars);
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        localLinkRefProcessor.updateNodeElements(document, node);
        assertThat(node).isEqualToComparingFieldByField(copy);
    }

    @Test
    public void testRelativeLinkProcessorUsesUrlForInlineTextWhenNoLinkText(){
        BasedSequence chars = BasedSequenceImpl.of("[](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThat(localLinkRefProcessor.adjustInlineText(document, node)).isEqualTo(node.getUrl());
    }

    @Test
    public void testRelativeLinkProcessorUsesTextForInlineTextWhenAvailable(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThat(localLinkRefProcessor.adjustInlineText(document, node)).isEqualTo(node.getText());
    }

    @Test
    public void testRelativeLinkProcessorAdjustInlineTextChecksForRelativeLinkNode(){
        Document document = mock(Document.class);
        Node node = mock(Node.class);
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        assertThatThrownBy(() -> localLinkRefProcessor.adjustInlineText(document, node)).isInstanceOf(UncheckedAlexandriaException.class);
    }

    @Test
    public void testRelativeLinkProcessorCreatesRelativeLinkNodes(){
        BasedSequence chars = BasedSequenceImpl.of("[](https://www.google.com)");
        LocalLinkRefProcessor localLinkRefProcessor = new LocalLinkRefProcessor();
        Node node = localLinkRefProcessor.createNode(chars);
        assertThat(node).isInstanceOf(Link.class);
        assertThat(node).isEqualToComparingFieldByField(new Link(chars));
    }

    @Test
    public void testRelativeLinkProcessorFactoryCreatesRelativeLinkRefProcessor(){
        Document document = mock(Document.class);
        when(document.get(any())).thenReturn(true);
        LinkRefProcessor processor = new LocalLinkRefProcessor.Factory(alexandriaFlexmarkLinkResolver)
                .create(document);
        assertThat(processor).isInstanceOf(LocalLinkRefProcessor.class);
    }

    @Test
    public void testRelativeLinkProcessorFactoryDoesntWantNesterBrackets(){
        LinkRefProcessorFactory factory = new LocalLinkRefProcessor.Factory(alexandriaFlexmarkLinkResolver);
        assertThat(factory.getBracketNestingLevel(mock(DataHolder.class))).isEqualTo(0);
    }

    @Test
    public void testRelativeLinkProcessorFactoryDoesntWantExclaimation(){
        LinkRefProcessorFactory factory = new LocalLinkRefProcessor.Factory(alexandriaFlexmarkLinkResolver);
        assertThat(factory.getWantExclamationPrefix(mock(DataHolder.class))).isFalse();
    }


}
