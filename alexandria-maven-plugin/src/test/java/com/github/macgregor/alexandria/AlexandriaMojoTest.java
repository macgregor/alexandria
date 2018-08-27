package com.github.macgregor.alexandria;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AlexandriaMojoTest {

    private MavenProject childProject = mock(MavenProject.class);
    private MavenProject parentProject = mock(MavenProject.class);
    private MavenSession session = mock(MavenSession.class);
    private Log log = mock(Log.class);
    private Context context = spy(new Context());
    private Alexandria alexandria = spy(new Alexandria());
    private TestAlexandriaMojo testAlexandriaMojo = new TestAlexandriaMojo();

    public static class TestAlexandriaMojo extends AlexandriaMojo{
        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
            return;
        }
    }

    @Before
    public void setup() throws IOException {
        when(childProject.getBasedir()).thenReturn(new File("childProject"));
        when(childProject.getParent()).thenReturn(parentProject);
        when(parentProject.getBasedir()).thenReturn(new File("parent"));
        when(parentProject.getParent()).thenReturn(null);
        when(session.getExecutionRootDirectory()).thenReturn(new File("parent").toString());

        alexandria.context(context);
        doReturn(alexandria).when(alexandria).context(any());
        testAlexandriaMojo.alexandria(alexandria);
        testAlexandriaMojo.project(childProject);
        testAlexandriaMojo.mavenSession(session);
        testAlexandriaMojo.setLog(log);
        testAlexandriaMojo.outputPath("foo");
    }

    @Test
    public void testRootDirFromParentContext(){
        testAlexandriaMojo.project(parentProject);
        assertThat(testAlexandriaMojo.rootDir()).isEqualTo(parentProject.getBasedir().toString());
    }

    @Test
    public void testRootDirFromChildContext(){
        testAlexandriaMojo.project(childProject);
        assertThat(testAlexandriaMojo.rootDir()).isEqualTo(parentProject.getBasedir().toString());
    }

    @Test
    public void testIsExecutionRootFromChild(){
        testAlexandriaMojo.project(childProject);
        assertThat(testAlexandriaMojo.isExecutionRoot()).isFalse();
    }

    @Test
    public void testIsExecutionRootFromParent(){
        testAlexandriaMojo.project(parentProject);
        assertThat(testAlexandriaMojo.isExecutionRoot()).isTrue();
    }

    @Test
    public void testLogConfigPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).configPath();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogProjectBaseAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).projectBase();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogSearchPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).searchPath();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogOutputPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).outputPath();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogIncludeAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).include();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogExcludeAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).exclude();
        verify(log, times(6)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testInitDoesntOverrideInputDir() throws IOException {
        testAlexandriaMojo.inputs(Collections.singletonList("foo"));
        testAlexandriaMojo.init();
        assertThat(testAlexandriaMojo.inputs()).containsExactlyInAnyOrder("foo");
        assertThat(context.searchPath()).containsExactlyInAnyOrder(Paths.get("foo"));
    }

    @Test
    public void testInitInputDirDefaultsToRootDir() throws IOException {
        testAlexandriaMojo.init();
        assertThat(testAlexandriaMojo.inputs()).containsExactlyInAnyOrder(parentProject.getBasedir().toString());
        assertThat(context.searchPath()).containsExactlyInAnyOrder(parentProject.getBasedir().toPath());
    }

    @Test
    public void testInitDoesntOverrideConfigPath() throws IOException {
        testAlexandriaMojo.configPath("foo");
        testAlexandriaMojo.init();
        assertThat(testAlexandriaMojo.configPath()).isEqualTo("foo");
        verify(alexandria, times(1)).context(context);
    }

    @Test
    public void testInitConfigPathDefaultsToRootDirDotAlexandria() throws IOException {
        testAlexandriaMojo.init();
        assertThat(testAlexandriaMojo.configPath()).isEqualTo(new File(parentProject.getBasedir(), ".alexandria").toString());
        assertThat(alexandria.context().configPath()).isEqualTo(new File(parentProject.getBasedir(), ".alexandria").toString());
    }

    @Test
    public void testInitSetsContextSearchPath() throws IOException {
        testAlexandriaMojo.inputs(Collections.singletonList("foo"));
        testAlexandriaMojo.init();
        assertThat(context.searchPath()).containsExactlyInAnyOrder(Paths.get("foo"));
    }

    @Test
    public void testInitSetsOutputPath() throws IOException {
        testAlexandriaMojo.outputPath("foo");
        testAlexandriaMojo.init();
        assertThat(context.outputPath().get()).isEqualTo(Paths.get("foo"));
    }

    @Test
    public void testInitDoesntSetIncludeWhenEmpty() throws IOException {
        testAlexandriaMojo.includes(Collections.emptyList());
        testAlexandriaMojo.init();
        verify(context, times(0)).include(anyList());
    }

    @Test
    public void testInitSetsInclude() throws IOException {
        testAlexandriaMojo.includes(Collections.singletonList("foo"));
        testAlexandriaMojo.init();
        assertThat(context.include()).containsExactlyInAnyOrder("foo");
    }

    @Test
    public void testInitDoesntSetExcludeWhenEmpty() throws IOException {
        testAlexandriaMojo.excludes(Collections.emptyList());
        testAlexandriaMojo.init();
        verify(context, times(0)).exclude(anyList());
    }

    @Test
    public void testInitSetsExclude() throws IOException {
        testAlexandriaMojo.excludes(Collections.singletonList("foo"));
        testAlexandriaMojo.init();
        assertThat(context.exclude()).containsExactlyInAnyOrder("foo");
    }
}
