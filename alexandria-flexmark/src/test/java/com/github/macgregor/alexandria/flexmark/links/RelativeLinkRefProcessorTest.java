package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.exceptions.UncheckedAlexandriaException;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.parser.LinkRefProcessor;
import com.vladsch.flexmark.parser.LinkRefProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.Range;
import org.junit.Test;

import java.nio.file.InvalidPathException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelativeLinkRefProcessorTest {

    @Test
    public void testRangeEmptyString(){
        BasedSequence chars = BasedSequenceImpl.of("");
        Range range = RelativeLinkRefProcessor.range(chars, "");
        assertThat(range).isEqualTo(new Range(0, 0));
    }

    @Test
    public void testRangeLengthOneString(){
        BasedSequence chars = BasedSequenceImpl.of("[");
        Range range = RelativeLinkRefProcessor.range(chars, "[");
        assertThat(range).isEqualTo(new Range(0, 1));
    }

    @Test
    public void testRangeLengthLongString(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](./foo/bar.md)");
        Range range = RelativeLinkRefProcessor.range(chars, "link text");
        assertThat(range).isEqualTo(new Range(1, 10));
    }

    @Test
    public void testRangeStringNotFound(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](./foo/bar.md)");
        Range range = RelativeLinkRefProcessor.range(chars, "red");
        assertThat(range).isEqualTo(new Range(-1, 2));
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinks(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](./foo.txt)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isTrue();
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinkNoText(){
        BasedSequence chars = BasedSequenceImpl.of("[](./foo.txt)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isTrue();
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinksNoDot(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](foo.txt)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isTrue();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchNonGithubStyleLink(){
        BasedSequence chars = BasedSequenceImpl.of("[link text|./foo.txt]");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchUrl(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](https://www.google.com)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchAbsolutePath(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](/foo/bar.txt)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchNull(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(BasedSequence.NULL)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchEmpty(){
        BasedSequence chars = BasedSequenceImpl.of("");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isMatch(chars)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntWantNesterBrackets(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.getBracketNestingLevel()).isEqualTo(0);
    }

    @Test
    public void testRelativeLinkProcessorDoesntWantExclaimation(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.getWantExclamationPrefix()).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntAllowDelimiters(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.allowDelimiters(mock(BasedSequence.class), mock(Document.class), mock(Node.class)))
                .isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntUpdateNodeElements(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        node.getUrl();
        Link copy = new Link(chars);
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        relativeLinkRefProcessor.updateNodeElements(document, node);
        assertThat(node).isEqualToComparingFieldByField(copy);
    }

    @Test
    public void testRelativeLinkProcessorUsesUrlForInlineTextWhenNoLinkText(){
        BasedSequence chars = BasedSequenceImpl.of("[](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.adjustInlineText(document, node)).isEqualTo(node.getUrl());
    }

    @Test
    public void testRelativeLinkProcessorUsesTextForInlineTextWhenAvailable(){
        BasedSequence chars = BasedSequenceImpl.of("[link text](https://www.google.com)");
        Document document = mock(Document.class);
        Link node = new Link(chars);
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.adjustInlineText(document, node)).isEqualTo(node.getText());
    }

    @Test
    public void testRelativeLinkProcessorAdjustInlineTextChecksForRelativeLinkNode(){
        Document document = mock(Document.class);
        Node node = mock(Node.class);
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThatThrownBy(() -> relativeLinkRefProcessor.adjustInlineText(document, node)).isInstanceOf(UncheckedAlexandriaException.class);
    }

    @Test
    public void testRelativeLinkProcessorSetsRelativeLinkOptions(){
        Document document = mock(Document.class);
        when(document.get(any())).thenReturn(true);
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor(document);
        assertThat(relativeLinkRefProcessor.getOptions().disableRendering).isTrue();
    }

    @Test
    public void testRelativeLinkProcessorCreatesRelativeLinkNodes(){
        BasedSequence chars = BasedSequenceImpl.of("[](https://www.google.com)");
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        Node node = relativeLinkRefProcessor.createNode(chars);
        assertThat(node).isInstanceOf(Link.class);
        assertThat(node).isEqualToComparingFieldByField(new Link(chars));
    }

    @Test
    public void testRelativeLinkProcessorFactoryCreatesRelativeLinkRefProcessor(){
        Document document = mock(Document.class);
        when(document.get(any())).thenReturn(true);
        LinkRefProcessor processor = new RelativeLinkRefProcessor.Factory()
                .create(document);
        assertThat(processor).isInstanceOf(RelativeLinkRefProcessor.class);
    }

    @Test
    public void testRelativeLinkProcessorFactoryDoesntWantNesterBrackets(){
        LinkRefProcessorFactory factory = new RelativeLinkRefProcessor.Factory();
        assertThat(factory.getBracketNestingLevel(mock(DataHolder.class))).isEqualTo(0);
    }

    @Test
    public void testRelativeLinkProcessorFactoryDoesntWantExclaimation(){
        LinkRefProcessorFactory factory = new RelativeLinkRefProcessor.Factory();
        assertThat(factory.getWantExclamationPrefix(mock(DataHolder.class))).isFalse();
    }

    @Test
    public void testIsAbsoluteNPE(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isAbsolute(null)).isTrue();
    }

    @Test
    public void testIsAbsoluteDetectsAbsolutePath(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        assertThat(relativeLinkRefProcessor.isAbsolute(BasedSequenceImpl.of("/foo/bar.txt"))).isTrue();
    }

    @Test
    public void testIsAbsoluteInvalidPath(){
        RelativeLinkRefProcessor relativeLinkRefProcessor = new RelativeLinkRefProcessor();
        BasedSequenceImpl basedSequence = mock(BasedSequenceImpl.class);
        when(basedSequence.toString()).thenThrow(new InvalidPathException("", ""));
        assertThat(relativeLinkRefProcessor.isAbsolute(basedSequence)).isTrue();
    }
}
