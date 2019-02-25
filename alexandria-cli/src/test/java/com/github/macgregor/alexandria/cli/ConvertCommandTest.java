package com.github.macgregor.alexandria.cli;

import com.github.macgregor.alexandria.Alexandria;
import com.github.macgregor.alexandria.Context;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class ConvertCommandTest {

    @Test
    public void testConvertIsCalled() throws Exception {
        ConvertCommand testCommand = Mockito.spy(new ConvertCommand());
        Alexandria alexandria = spy(new Alexandria());
        Context context = spy(new Context());
        alexandria.context(context);
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        doReturn(alexandria).when(testCommand).alexandria();
        testCommand.call();
        verify(alexandria, times(1)).convert();
    }
}
