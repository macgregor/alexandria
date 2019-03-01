package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.Alexandria;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import com.github.macgregor.alexandria.maven.SyncMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class SyncMojoTest {

    private MavenProject childProject = mock(MavenProject.class);
    private MavenProject parentProject = mock(MavenProject.class);
    private MavenSession session = mock(MavenSession.class);
    private Log log = mock(Log.class);
    private Context context = spy(new Context());
    private Alexandria alexandria = spy(new Alexandria());
    private SyncMojo syncMojo = spy(new SyncMojo());

    @Before
    public void setup() throws IOException, BatchProcessException {
        when(childProject.getBasedir()).thenReturn(new File("childProject"));
        when(childProject.getParent()).thenReturn(parentProject);
        when(parentProject.getBasedir()).thenReturn(new File("parent"));
        when(parentProject.getParent()).thenReturn(null);
        when(session.getExecutionRootDirectory()).thenReturn(new File("parent").toString());

        alexandria.context(context);
        doReturn(alexandria).when(alexandria).context(any());
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        syncMojo.alexandria(alexandria);
        syncMojo.project(childProject);
        syncMojo.mavenSession(session);
        syncMojo.setLog(log);
        syncMojo.outputPath("foo");
    }

    @Test
    public void testSyncDoesntRunOnChildProject() throws MojoFailureException, MojoExecutionException, IOException {
        syncMojo.project(childProject);
        syncMojo.execute();
        verify(syncMojo, times(0)).init();
        verify(syncMojo, times(0)).logContext();
        verify(syncMojo, times(0)).alexandria();
    }

    @Test
    public void testSyncRunsOnRootProject() throws MojoFailureException, MojoExecutionException, IOException {
        syncMojo.project(parentProject);
        syncMojo.execute();
        verify(syncMojo, times(1)).init();
        verify(syncMojo, times(1)).logContext();
        verify(syncMojo, atLeastOnce()).alexandria();
    }

    @Test
    public void testSyncCallsConvert() throws MojoFailureException, MojoExecutionException, IOException, BatchProcessException {
        syncMojo.project(parentProject);
        syncMojo.execute();
        verify(syncMojo, times(1)).init();
        verify(syncMojo, times(1)).logContext();
        verify(syncMojo, atLeastOnce()).alexandria();
        verify(alexandria, times(1)).syncWithRemote();
    }

    @Test
    public void testSyncWrapsIOExceptions() throws IOException {
        syncMojo.failBuild(true);
        syncMojo.project(parentProject);
        doThrow(IOException.class).when(syncMojo).init();
        assertThatThrownBy(() -> syncMojo.execute()).isInstanceOf(MojoFailureException.class);
    }

    @Test
    public void testSyncWrapsBatchProcessException() throws IOException {
        syncMojo.failBuild(true);
        syncMojo.project(parentProject);
        doThrow(BatchProcessException.class).when(alexandria).syncWithRemote();
        assertThatThrownBy(() -> syncMojo.execute()).isInstanceOf(MojoFailureException.class);
    }

    @Test
    public void testSyncDoesntThrowErrorWhenFailBuildsSetToFalse() throws AlexandriaException, MojoFailureException, MojoExecutionException {
        syncMojo.failBuild(false);
        syncMojo.project(parentProject);
        doThrow(BatchProcessException.class).when(alexandria).syncWithRemote();
        syncMojo.execute();
    }
}
