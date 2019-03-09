package com.github.macgregor.alexandria.flexmark;

import com.github.macgregor.alexandria.flexmark.links.LocalLinkRefProcessor;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.Range;
import org.junit.Test;

import java.nio.file.InvalidPathException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlexmarkUtilsTest {
    @Test
    public void testRangeEmptyString(){
        Range range = FlexmarkUtils.range("", "");
        assertThat(range).isEqualTo(new Range(0, 0));
    }

    @Test
    public void testRangeLengthOneString(){
        Range range = FlexmarkUtils.range("[", "[");
        assertThat(range).isEqualTo(new Range(0, 1));
    }

    @Test
    public void testRangeLengthLongString(){
        Range range = FlexmarkUtils.range("[link text](./foo/bar.md)", "link text");
        assertThat(range).isEqualTo(new Range(1, 10));
    }

    @Test
    public void testRangeStringNotFound(){
        Range range = FlexmarkUtils.range("[link text](./foo/bar.md)", "red");
        assertThat(range).isEqualTo(new Range(-1, 2));
    }

    @Test
    public void testIsAbsoluteNPE(){
        assertThat(FlexmarkUtils.isAbsolute(null)).isTrue();
    }

    @Test
    public void testIsAbsoluteDetectsAbsolutePath(){
        assertThat(FlexmarkUtils.isAbsolute("/foo/bar.txt")).isTrue();
    }
}
