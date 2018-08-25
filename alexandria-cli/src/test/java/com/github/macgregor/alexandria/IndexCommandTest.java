package com.github.macgregor.alexandria;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class IndexCommandTest {

    @Test
    public void testIndexIsCalled() throws Exception {
        IndexCommand testCommand = spy(new IndexCommand());
        Alexandria alexandria = spy(new Alexandria());
        Context context = spy(new Context());
        alexandria.context(context);
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        doReturn(alexandria).when(testCommand).alexandria();
        testCommand.call();
        verify(alexandria, times(1)).index();
    }
}
