package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
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
import static org.mockito.Mockito.*;

public class ConvertMojoTest {

    private MavenProject childProject = mock(MavenProject.class);
    private MavenProject parentProject = mock(MavenProject.class);
    private MavenSession session = mock(MavenSession.class);
    private Log log = mock(Log.class);
    private Context context = spy(new Context());
    private Alexandria alexandria = spy(new Alexandria());
    private ConvertMojo convertMojo = spy(new ConvertMojo());

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
        convertMojo.alexandria(alexandria);
        convertMojo.project(childProject);
        convertMojo.mavenSession(session);
        convertMojo.setLog(log);
        convertMojo.outputPath("foo");
    }

    @Test
    public void testConvertDoesntRunOnChildProject() throws MojoFailureException, MojoExecutionException, IOException {
        convertMojo.project(childProject);
        convertMojo.execute();
        verify(convertMojo, times(0)).init();
        verify(convertMojo, times(0)).logContext();
        verify(convertMojo, times(0)).alexandria();
    }

    @Test
    public void testConvertRunsOnRootProject() throws MojoFailureException, MojoExecutionException, IOException {
        convertMojo.project(parentProject);
        convertMojo.execute();
        verify(convertMojo, times(1)).init();
        verify(convertMojo, times(1)).logContext();
        verify(convertMojo, atLeastOnce()).alexandria();
    }

    @Test
    public void testConvertCallsConvert() throws MojoFailureException, MojoExecutionException, IOException, BatchProcessException {
        convertMojo.project(parentProject);
        convertMojo.execute();
        verify(convertMojo, times(1)).init();
        verify(convertMojo, times(1)).logContext();
        verify(convertMojo, atLeastOnce()).alexandria();
        verify(alexandria, times(1)).convert();
    }

    @Test
    public void testConvertWrapsIOExceptions() throws IOException {
        convertMojo.failBuild(true);
        convertMojo.project(parentProject);
        doThrow(IOException.class).when(convertMojo).init();
        assertThatThrownBy(() -> convertMojo.execute()).isInstanceOf(MojoFailureException.class);
    }

    @Test
    public void testConvertWrapsBatchProcessException() throws IOException {
        convertMojo.failBuild(true);
        convertMojo.project(parentProject);
        doThrow(BatchProcessException.class).when(alexandria).convert();
        assertThatThrownBy(() -> convertMojo.execute()).isInstanceOf(MojoFailureException.class);
    }

    @Test
    public void testSyncDoesntThrowErrorWhenFailBuildsSetToFalse() throws AlexandriaException, MojoFailureException, MojoExecutionException {
        convertMojo.failBuild(false);
        convertMojo.project(parentProject);
        doThrow(BatchProcessException.class).when(alexandria).syncWithRemote();
        convertMojo.execute();
    }
}
