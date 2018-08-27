package com.github.macgregor.alexandria;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AlexandriaCommandTest {

    public static class TestCommand extends AlexandriaCommand{
        @Override
        public Void call() throws Exception {
            return null;
        }
    }

    @Test
    public void testLogVerbosity0(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.OFF);
    }

    @Test
    public void testLogVerbosity1(){
        String[] args = {"-v"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    public void testLogVerbosity2(){
        String[] args = {"-vv"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testLogVerbosity3(){
        String[] args = {"-vvv"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    public void testLogVerbosity4(){
        String[] args = {"-vvvv"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
    }

    @Test
    public void testLogVerbosity5(){
        String[] args = {"-vvvvv"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        command.configureLogging();
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria");
        assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
    }

    @Test
    public void testAlexandriaInitSetsContextConfigPath() throws IOException {
        TestCommand testCommand = new TestCommand();
        Path configPath = Paths.get(System.getProperty("user.dir"), ".alexandria");
        assertThat(testCommand.init().context().configPath()).isEqualTo(configPath);
    }

    @Test
    public void testAlexandriaInitSetsContextInput() throws IOException {
        TestCommand testCommand = new TestCommand();
        testCommand.input(Arrays.asList("foo"));
        assertThat(testCommand.init().context().searchPath()).containsExactlyInAnyOrder(Paths.get("foo"));
    }

    @Test
    public void testAlexandriaInitSetsContextOutput() throws IOException {
        TestCommand testCommand = new TestCommand();
        testCommand.outputPath("foo");
        assertThat(testCommand.init().context().outputPath().get()).isEqualTo(Paths.get("foo"));
    }

    @Test
    public void testAlexandriaInitSetsContextInclude() throws IOException {
        TestCommand testCommand = new TestCommand();
        testCommand.include(Arrays.asList("foo"));
        assertThat(testCommand.init().context().include()).containsExactlyInAnyOrder("foo");
    }

    @Test
    public void testAlexandriaInitSetsContextExclude() throws IOException {
        TestCommand testCommand = new TestCommand();
        testCommand.exclude(Arrays.asList("foo"));
        assertThat(testCommand.init().context().exclude()).containsExactlyInAnyOrder("foo");
    }

    @Test
    public void testInitSetsTimeout() throws IOException {
        TestCommand testCommand = new TestCommand();
        testCommand.timeout(45);
        assertThat(testCommand.init().context().config().remote().requestTimeout()).isEqualTo(45);
    }

    @Test
    public void testInitSetsTimeoutDefault() throws IOException {
        TestCommand testCommand = new TestCommand();
        assertThat(testCommand.init().context().config().remote().requestTimeout()).isEqualTo(30);
    }

    @Test
    public void testLogContextLogsConfigPath(){
        Context context = spyContext();
        verify(context, times(1)).configPath();
    }

    @Test
    public void testLogContextLogsProjectBase(){
        Context context = spyContext();
        verify(context, times(1)).projectBase();
    }

    @Test
    public void testLogContextLogsSearchPath(){
        Context context = spyContext();
        verify(context, times(1)).searchPath();
    }

    @Test
    public void testLogContextLogsOutput(){
        Context context = spyContext();
        verify(context, times(1)).outputPath();
    }

    @Test
    public void testLogContextLogsInclude(){
        Context context = spyContext();
        verify(context, times(1)).include();
    }

    @Test
    public void testLogContextLogsExclude(){
        Context context = spyContext();
        verify(context, times(1)).exclude();
    }

    private Context spyContext(){
        TestCommand testCommand = spy(new TestCommand());
        Alexandria alexandria = spy(new Alexandria());
        Context context = spy(new Context());
        alexandria.context(context);
        testCommand.alexandria(alexandria);
        testCommand.logContext();
        return context;
    }
}
