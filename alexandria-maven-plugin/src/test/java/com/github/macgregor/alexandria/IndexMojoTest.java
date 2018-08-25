package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class IndexMojoTest {

    private MavenProject childProject = mock(MavenProject.class);
    private MavenProject parentProject = mock(MavenProject.class);
    private MavenSession session = mock(MavenSession.class);
    private Log log = mock(Log.class);
    private Context context = spy(new Context());
    private Alexandria alexandria = spy(new Alexandria());
    private IndexMojo indexMojo = spy(new IndexMojo());

    @Before
    public void setup() throws IOException, BatchProcessException {
        when(childProject.getBasedir()).thenReturn(new File("childProject"));
        when(childProject.getParent()).thenReturn(parentProject);
        when(parentProject.getBasedir()).thenReturn(new File("parent"));
        when(parentProject.getParent()).thenReturn(null);
        when(session.getExecutionRootDirectory()).thenReturn(new File("parent").toString());

        alexandria.context(context);
        doReturn(alexandria).when(alexandria).load(anyString());
        doReturn(alexandria).when(alexandria).index();
        doReturn(alexandria).when(alexandria).convert();
        doReturn(alexandria).when(alexandria).syncWithRemote();
        indexMojo.alexandria(alexandria);
        indexMojo.project(childProject);
        indexMojo.mavenSession(session);
        indexMojo.setLog(log);
        indexMojo.outputPath("foo");
    }

    @Test
    public void testIndexDoesntRunOnChildProject() throws MojoFailureException, MojoExecutionException, IOException {
        indexMojo.project(childProject);
        indexMojo.execute();
        verify(indexMojo, times(0)).init();
        verify(indexMojo, times(0)).logContext();
        verify(indexMojo, times(0)).alexandria();
    }

    @Test
    public void testIndexRunsOnRootProject() throws MojoFailureException, MojoExecutionException, IOException {
        indexMojo.project(parentProject);
        indexMojo.execute();
        verify(indexMojo, times(1)).init();
        verify(indexMojo, times(1)).logContext();
        verify(indexMojo, atLeastOnce()).alexandria();
    }

    @Test
    public void testIndexCallsConvert() throws MojoFailureException, MojoExecutionException, IOException, BatchProcessException {
        indexMojo.project(parentProject);
        indexMojo.execute();
        verify(indexMojo, times(1)).init();
        verify(indexMojo, times(1)).logContext();
        verify(indexMojo, atLeastOnce()).alexandria();
        verify(alexandria, times(1)).index();
    }

    @Test
    public void testIndexWrapsIOExceptions() throws IOException {
        indexMojo.project(parentProject);
        doThrow(IOException.class).when(indexMojo).init();
        assertThatThrownBy(() -> indexMojo.execute()).isInstanceOf(MojoFailureException.class);
    }

    @Test
    public void testIndexWrapsBatchProcessException() throws IOException {
        indexMojo.project(parentProject);
        doThrow(BatchProcessException.class).when(alexandria).index();
        assertThatThrownBy(() -> indexMojo.execute()).isInstanceOf(MojoFailureException.class);
    }
}
