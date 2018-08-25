package com.github.macgregor.alexandria;

import picocli.CommandLine;

@CommandLine.Command(description = "Converts markdown files in the Alexandria metadata index into html files.",
        name = "convert", mixinStandardHelpOptions = true)
public class ConvertCommand extends AlexandriaCommand {

    @Override
    public Void call() throws Exception {
        configureLogging();
        init();
        logContext();
        alexandria().convert();
        return null;
    }
}
