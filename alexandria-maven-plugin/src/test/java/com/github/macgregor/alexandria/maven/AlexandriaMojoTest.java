package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.Alexandria;
import com.github.macgregor.alexandria.Context;
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
    private MavenProject remoteParentProject = mock(MavenProject.class);
    private MavenSession session = mock(MavenSession.class);
    private Log log = mock(Log.class);
    private Context context = spy(new Context());
    private Alexandria alexandria = spy(new Alexandria());
    private TestAlexandriaMojo testAlexandriaMojo = new TestAlexandriaMojo();

    public static class TestAlexandriaMojo extends AlexandriaMojo {
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
        when(parentProject.getParent()).thenReturn(remoteParentProject);
        when(remoteParentProject.getBasedir()).thenReturn(null);
        when(remoteParentProject.getParent()).thenReturn(null);
        when(session.getExecutionRootDirectory()).thenReturn(new File("parent").toString());

        context.configPath(new File(parentProject.getBasedir(), ".alexandria").toPath());
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
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogProjectBaseAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).projectBase();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogSearchPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).searchPath();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogOutputPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).outputPath();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogIncludeAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).include();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogExcludeAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).exclude();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogDisclaimerFooterEnabledAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).disclaimerFooterEnabled();
        verify(log, times(8)).debug(anyString());
        verify(log, times(0)).info(anyString());
    }

    @Test
    public void testLogDisclaimerFooterPathAtDebug(){
        testAlexandriaMojo.logContext();
        verify(context, times(1)).disclaimerFooterPath();
        verify(log, times(8)).debug(anyString());
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
    public void testInitInputNullDirDefaultsToRootDir() throws IOException {
        testAlexandriaMojo.inputs = null;
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

    @Test
    public void testInitSetsTimeoutDefault() throws IOException {
        testAlexandriaMojo.init();
        assertThat(context.config().remote().requestTimeout()).isEqualTo(30);
    }

    @Test
    public void testInitSetsTimeout() throws IOException {
        testAlexandriaMojo.timeout(45);
        testAlexandriaMojo.init();
        assertThat(context.config().remote().requestTimeout()).isEqualTo(45);
    }

    @Test
    public void testFailBuildDefaultsToFalse(){
        assertThat(testAlexandriaMojo.failBuild()).isFalse();
    }

    @Test
    public void testDisclaimerFooterEnabledDefaultsToTrue() throws IOException {
        testAlexandriaMojo.init();
        assertThat(context.disclaimerFooterEnabled()).isTrue();
    }

    @Test
    public void testDisclaimerFooterEnabledOverride() throws IOException {
        testAlexandriaMojo.disclaimerFooterEnabled = false;
        testAlexandriaMojo.init();
        assertThat(context.disclaimerFooterEnabled()).isFalse();
    }

    @Test
    public void testDisclaimerFooterPathDefaultsToNull() throws IOException {
        testAlexandriaMojo.init();
        assertThat(context.disclaimerFooterPath()).isEmpty();
    }

    @Test
    public void testDisclaimerFooterPathOverride() throws IOException {
        testAlexandriaMojo.disclaimerFooterPath("foo");
        testAlexandriaMojo.init();
        assertThat(context.disclaimerFooterPath()).isPresent();
        assertThat(context.disclaimerFooterPath().get()).isEqualTo(Paths.get("foo"));
    }
}
