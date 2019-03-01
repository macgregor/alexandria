package com.github.macgregor.alexandria.cli;

import org.junit.Test;
import picocli.CommandLine;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class AlexandriaCommandConfigTest {

    public static class TestCommand extends AlexandriaCommand {
        @Override
        public Void call() throws Exception {
            return null;
        }
    }

    @Test
    public void testDefaultVerbosity(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.verbosity()).isEmpty();
    }

    @Test
    public void testVerbosityShort(){
        String[] args = {"-v"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true);
    }

    @Test
    public void testVerbosityShortMultipleArgs(){
        String[] args = {"-v", "-v"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true, true);
    }

    @Test
    public void testVerbosityMixedMultipleSingeArg(){
        String[] args = {"-vv"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true, true);
    }

    @Test
    public void testVerbosityLong(){
        String[] args = {"--verbose"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true);
    }

    @Test
    public void testVerbosityLongMultiple(){
        String[] args = {"--verbose", "--verbose"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true, true);
    }

    @Test
    public void testVerbosityMixedMultiple(){
        String[] args = {"-v", "--verbose"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.verbosity()).containsExactly(true, true);
    }

    @Test
    public void testDefaultConfigPath(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.configPath()).isEqualTo(Paths.get(System.getProperty("user.dir"), ".alexandria").toString());
    }

    @Test
    public void testConfigPathShort(){
        String[] args = {"-c", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.configPath()).isEqualTo("foo/bar");
    }

    @Test
    public void testConfigPathLong(){
        String[] args = {"--config", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.configPath()).isEqualTo("foo/bar");
    }

    @Test
    public void testDefaultOutput(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.outputPath()).isNull();
    }

    @Test
    public void testOutputShort(){
        String[] args = {"-o", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.outputPath()).isEqualTo("foo/bar");
    }

    @Test
    public void testOutputLong(){
        String[] args = {"--output", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.outputPath()).isEqualTo("foo/bar");
    }

    @Test
    public void testDefaultInput(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.input()).containsExactlyInAnyOrder(System.getProperty("user.dir"));
    }

    @Test
    public void testInputShort(){
        String[] args = {"-p", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.input()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testInputShortMultiple(){
        String[] args = {"-p", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.input()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testInputLong(){
        String[] args = {"--input", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.input()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testInputLongMultiple(){
        String[] args = {"--input", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.input()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testDefaultInclude(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.include()).isEmpty();
    }

    @Test
    public void testIncludeShort(){
        String[] args = {"-i", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.include()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testIncludeShortMultiple(){
        String[] args = {"-i", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.include()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testIncludeLong(){
        String[] args = {"--include", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.include()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testIncludeLongMultiple(){
        String[] args = {"--include", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.include()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testDefaultExclude(){
        TestCommand command = CommandLine.populateCommand(new TestCommand(), new String[]{});
        assertThat(command.exclude()).isEmpty();
    }

    @Test
    public void testExcludeShort(){
        String[] args = {"-e", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.exclude()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testExcludeShortMultiple(){
        String[] args = {"-e", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.exclude()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testExcludeLong(){
        String[] args = {"--exclude", "foo/bar"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.exclude()).containsExactlyInAnyOrder("foo/bar");
    }

    @Test
    public void testExcludeLongMultiple(){
        String[] args = {"--exclude", "foo/bar", "baz"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.exclude()).containsExactlyInAnyOrder("foo/bar", "baz");
    }

    @Test
    public void testTimeoutShort(){
        String[] args = {"-t", "45"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.timeout()).isEqualTo(45);
    }

    @Test
    public void testTimeoutLong(){
        String[] args = {"--timeout", "45"};
        TestCommand command = CommandLine.populateCommand(new TestCommand(), args);
        assertThat(command.timeout()).isEqualTo(45);
    }
}
