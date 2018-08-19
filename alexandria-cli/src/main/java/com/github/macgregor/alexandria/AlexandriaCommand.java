package com.github.macgregor.alexandria;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public abstract class AlexandriaCommand implements Callable<Void> {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @CommandLine.Option(names = { "-v", "--verbose" }, description = {
            "Specify multiple -v options to increase verbosity.",
            "For example, `-v -v -v` or `-vvv`" })
    private boolean[] verbosity = new boolean[0];

    @CommandLine.Option(names = { "-c", "--config" }, description = "Alexandria config file path. Defaults to .alexandria in the current directory.")
    private String configPath = Paths.get(System.getProperty("user.dir"), ".alexandria").toString();

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output directory for converted files. If not specified, will convert file in place.")
    private String output;

    @CommandLine.Parameters(arity = "0..*", paramLabel = "INPUT_DIRECTORIES", description = "One or more directories to search for files in. Defaults to current directory.")
    private List<String> input = Arrays.asList(System.getProperty("user.dir"));

    @CommandLine.Parameters(arity = "0..*", paramLabel = "INCLUDES", description = "One or more file naming patterns to explicitly include. Defaults to *.md")
    private List<String> include = new ArrayList<>();

    @CommandLine.Parameters(arity = "0..*", paramLabel = "EXCLUDES", description = "One or more file naming patterns to explicitly exclude. Defaults to empty list (no exclusions).")
    private List<String> exclude = new ArrayList<>();

    public void configureLogging(){
        switch(verbosity.length){
            case 0:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.OFF);
            case 1:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.WARN);
                break;
            case 2:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.INFO);
                break;
            case 3:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.DEBUG);
                break;
            default:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.TRACE);
                break;

        }
    }

    public Context alexandriaContext() throws IOException {
        Context context = Alexandria.load(configPath);
        context.searchPath(input);
        context.output(Optional.ofNullable(output));
        if (include.size() > 0) {
            context.include(include);
        }
        if (exclude.size() > 0) {
            context.exclude(exclude);
        }
        return context;
    }

    public void logContext(Context context){
        log.info("Alexandria - config file: " + context.configPath());
        log.info("Alexandria - project base dir: " + context.projectBase());
        log.info("Alexandria - input directories: " + context.searchPath());
        log.info("Alexandria - output directory: " + context.output());
        log.info("Alexandria - include files: " + context.include());
        log.info("Alexandria - exclude files: " + context.exclude());
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }
}
