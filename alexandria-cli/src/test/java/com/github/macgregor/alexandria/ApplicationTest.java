package com.github.macgregor.alexandria;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class ApplicationTest {

    @Test
    public void testSyncIsCalled() throws Exception {
        Application application = spy(new Application());
        Alexandria alexandria = alexandriaSpy(application);
        application.call();
        verify(alexandria, times(1)).syncWithRemote();
    }

    @Test
    public void testIndexIsCalled() throws Exception {
        Application application = spy(new Application());
        Alexandria alexandria = alexandriaSpy(application);
        application.call();
        verify(alexandria, times(1)).index();
    }

    @Test
    public void testConvertIsCalled() throws Exception {
        Application application = spy(new Application());
        Alexandria alexandria = alexandriaSpy(application);
        application.call();
        verify(alexandria, times(1)).convert();
    }

    private Alexandria alexandriaSpy(Application application) throws Exception {
        Alexandria alexandria = spy(new Alexandria());
        Context context = spy(new Context());
        alexandria.context(context);
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        doReturn(alexandria).when(application).alexandria();
        return alexandria;
    }
}
