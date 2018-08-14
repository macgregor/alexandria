package com.github.macgregor.alexandria.remotes;


import com.github.macgregor.alexandria.Config;

import java.io.IOException;

public interface Remote {
    default void validateRemoteConfig() throws IllegalStateException{}
    default void validateDocumentMetadata(Config.DocumentMetadata metadata) throws IllegalStateException{}
    void create(Config.DocumentMetadata metadata) throws IOException;
    void update(Config.DocumentMetadata metadata) throws IOException;
    void delete(Config.DocumentMetadata metadata) throws IOException;
}
