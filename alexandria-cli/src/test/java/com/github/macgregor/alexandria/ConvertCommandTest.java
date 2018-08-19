package com.github.macgregor.alexandria;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class ConvertCommandTest {

    @Test
    public void testConvertIsCalled() throws Exception {
        ConvertCommand testCommand = spy(new ConvertCommand());
        Alexandria alexandria = spy(new Alexandria());
        Context context = spy(new Context());
        alexandria.context(context);
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        doReturn(alexandria).when(testCommand).getAlexandria();
        testCommand.call();
        verify(alexandria, times(1)).convert();
    }
}
