package com.github.macgregor.alexandria.remotes;


import com.github.macgregor.alexandria.AlexandriaConfig;

import java.io.IOException;

public interface Remote {
    default void validateRemoteConfig(AlexandriaConfig.RemoteConfig config) throws IllegalStateException{}
    default void validateDocumentMetadata(AlexandriaConfig.DocumentMetadata metadata) throws IllegalStateException{}
    void syncMetadata(AlexandriaConfig.DocumentMetadata metadata) throws IOException;
    void create(AlexandriaConfig.DocumentMetadata metadata) throws IOException;
    void update(AlexandriaConfig.DocumentMetadata metadata) throws IOException;
    void delete(AlexandriaConfig.DocumentMetadata metadata) throws IOException;
}
