package com.github.macgregor.alexandria;

import picocli.CommandLine;

@CommandLine.Command(description = "Create or update the Alexandria metadata index.",
        name = "index", mixinStandardHelpOptions = true)
public class IndexCommand extends AlexandriaCommand {

    @Override
    public Void call() throws Exception {
        configureLogging();
        init();
        logContext();
        getAlexandria().index();
        return null;
    }
}
