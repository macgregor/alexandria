package com.github.macgregor.alexandria;

import org.junit.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

public class ConvertCommandTest {

    @Test
    public void testMultipleInputDirs(){
        String[] args = { "foo/", "bar/" };
        ConvertCommand convertCommand = CommandLine.populateCommand(new ConvertCommand(), args);
        assertThat(convertCommand.getInput()).containsExactlyInAnyOrder("foo/", "bar/");
    }

    @Test
    public void testNoInputDirs(){
        ConvertCommand convertCommand = CommandLine.populateCommand(new ConvertCommand(), new String[]{});
    }

    @Test
    public void testOutputDirDefaultsToNull(){
        String[] args = { "foo/", "bar/" };
        ConvertCommand convertCommand = CommandLine.populateCommand(new ConvertCommand(), args);
        assertThat(convertCommand.getOutput()).isNull();
    }

    @Test
    public void testOutputDirSpecifiedLong(){
        String[] args = { "foo/", "bar/", "--output", "baz/"};
        ConvertCommand convertCommand = CommandLine.populateCommand(new ConvertCommand(), args);
        assertThat(convertCommand.getOutput()).isEqualTo("baz/");
    }

    @Test
    public void testOutputDirSpecifiedShort(){
        String[] args = { "foo/", "bar/", "-o", "baz/"};
        ConvertCommand convertCommand = CommandLine.populateCommand(new ConvertCommand(), args);
        assertThat(convertCommand.getOutput()).isEqualTo("baz/");
    }
}
