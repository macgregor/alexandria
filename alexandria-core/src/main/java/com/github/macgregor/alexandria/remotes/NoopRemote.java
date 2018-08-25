package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@NoArgsConstructor
public class NoopRemote implements Remote {

    @Override
    public void configure(Config.RemoteConfig config) {
        log.debug("Noop - Configuring remote.");
        return;
    }

    @Override
    public void create(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Creating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    @Override
    public void update(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Updating %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }

    @Override
    public void delete(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Noop - Deleting %s on remote.", metadata.sourcePath().toAbsolutePath().toString()));
        return;
    }
}
