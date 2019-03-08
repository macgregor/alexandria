package com.github.macgregor.alexandria.cli;

import ch.qos.logback.classic.Level;
import com.github.macgregor.alexandria.Alexandria;
import com.github.macgregor.alexandria.AlexandriaConvert;
import com.github.macgregor.alexandria.Context;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@Getter @Setter @Accessors(fluent = true)
public abstract class AlexandriaCommand implements Callable<Void> {

    @CommandLine.Option(names = { "-v", "--verbose" }, description = {
            "Specify multiple -v options to increase verbosity.",
            "For example, `-v -v -v` or `-vvv`" })
    private boolean[] verbosity = new boolean[0];

    @CommandLine.Option(names = { "-c", "--config" }, description = "Alexandria config file path. Defaults to .alexandria in the current directory.")
    private String configPath = Paths.get(System.getProperty("user.dir"), ".alexandria").toString();

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output directory for converted files. If not specified, will convert file in place.")
    private String outputPath;

    @CommandLine.Option(names = {"-p", "--input"}, arity = "1..*", description = "One or more directories to search for files in. Defaults to current directory.")
    private List<String> input = Arrays.asList(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-i", "--include"}, arity = "1..*", description = "One or more file naming patterns to explicitly include. Defaults to *.md")
    private List<String> include = new ArrayList<>();

    @CommandLine.Option(names = {"-e", "--exclude"}, arity = "1..*", description = "One or more file naming patterns to explicitly exclude. Defaults to empty list (no exclusions).")
    private List<String> exclude = new ArrayList<>();

    @CommandLine.Option(names = { "-t", "--timeout" }, description = "Timeout for remote request in seconds.")
    private Integer timeout = 30;

    @CommandLine.Option(names = {"--disclaimerFooterDisabled" }, description = "Disable Alexandria feature to add a disclaimer footer to each converted file warning the reader the document isnt the source and changes will be overwritten.")
    private boolean disclaimerFooterDisabled = false;

    @CommandLine.Option(names = {"--disclaimerFooterPath" }, description = "Optional path to a custom (markdown) file to use as the footer added to documents. Defaults to null (use Alexandria default).")
    private String disclaimerFooterPath;

    private Alexandria alexandria;

    public void configureLogging(){
        switch(verbosity.length){
            case 0:
                ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.github.macgregor.alexandria")) .setLevel(Level.OFF);
                break;
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

    public Alexandria init() throws IOException {
        alexandria = new Alexandria();
        alexandria.context(Context.load(configPath));
        alexandria.context().searchPath(input.stream().map(Paths::get).collect(Collectors.toList()));
        alexandria.context().outputPath(outputPath == null ? Optional.empty() : Optional.of(Paths.get(outputPath)));
        alexandria.context().config().remote().requestTimeout(timeout);
        alexandria.context().disclaimerFooterEnabled(!disclaimerFooterDisabled);
        if(disclaimerFooterPath != null){
            alexandria.context().disclaimerFooterPath(Optional.of(Paths.get(disclaimerFooterPath)));
        }
        if (include.size() > 0) {
            alexandria.context().include(include);
        }
        if (exclude.size() > 0) {
            alexandria.context().exclude(exclude);
        }
        return alexandria;
    }

    public void logContext(){
        log.info("Alexandria - config file: " + alexandria.context().configPath());
        log.info("Alexandria - project base dir: " + alexandria.context().projectBase());
        log.info("Alexandria - input directories: " + alexandria.context().searchPath());
        log.info("Alexandria - outputPath directory: " + alexandria.context().outputPath());
        log.info("Alexandria - disclaimer footer enabled : " + alexandria.context().disclaimerFooterEnabled());
        log.info("Alexandria - disclaimer footer path : " +
                alexandria.context().disclaimerFooterPath()
                        .orElse(Paths.get("classpath:"+AlexandriaConvert.DEFAULT_DISCLAIMER_FOOTER_FILE)));
        log.info("Alexandria - include files: " + alexandria.context().include());
        log.info("Alexandria - exclude files: " + alexandria.context().exclude());
    }
}
