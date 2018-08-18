package com.github.macgregor.alexandria;

import picocli.CommandLine;

@CommandLine.Command(description = "Sync local documents in the Alexandria metadata index with remote.",
        name = "sync", mixinStandardHelpOptions = true)
public class SyncCommand extends AlexandriaCommand {

    @Override
    public Void call() throws Exception {
        configureLogging();
        Config config = alexandriaConfig();
        logConfig(config);
        Alexandria.syncWithRemote(config);
        return null;
    }
}
