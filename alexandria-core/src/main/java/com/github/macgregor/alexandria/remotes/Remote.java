package com.github.macgregor.alexandria.remotes;


import com.github.macgregor.alexandria.Config;

import java.io.IOException;

/**
 * Defines an interface for interacting with a remote document source. See {@link JiveRemote} for an example.
 */
public interface Remote {

    /**
     * Called after instantiating the Remote to validate the configuration has all information required to use the remote.
     * @throws IllegalStateException The remote configuration is invalid.
     */
    default void validateRemoteConfig() throws IllegalStateException{}

    /**
     * Called for each document to validate that all metadata required by the remote exists.
     * @param metadata
     * @throws IllegalStateException The document metadata is invalid.
     */
    default void validateDocumentMetadata(Config.DocumentMetadata metadata) throws IllegalStateException{}

    /**
     * Called to create a new document on the remote. If the remote doesnt support native markdown, the metadata
     * should contain link to converted html file in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#convertedPath}
     * by the time this method is called. The implementation is responsible for updating the metadata with:
     *  - {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     *  - {@link com.github.macgregor.alexandria.Config.DocumentMetadata#createdOn}
     *  - {@link com.github.macgregor.alexandria.Config.DocumentMetadata#lastUpdated}
     *  - any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void create(Config.DocumentMetadata metadata) throws IOException;

    /**
     * Called to update an existing document on the remote. If the remote doesnt support native markdown, the metadata
     * should contain link to converted html file in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#convertedPath}
     * by the time this method is called. The implementation is responsible for updating the metadata with:
     *  - {@link com.github.macgregor.alexandria.Config.DocumentMetadata#lastUpdated}
     *  - any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void update(Config.DocumentMetadata metadata) throws IOException;

    /**
     * Called to delete an existing document on the remote. The implementation is responsible for updating the metadata with:
     *  - {@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn}
     *  - any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void delete(Config.DocumentMetadata metadata) throws IOException;
}
