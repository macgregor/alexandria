package com.github.macgregor.alexandria.remotes;


import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;

import java.io.IOException;

/**
 * Defines an interface for interacting with a remote document source. See {@link JiveRemote} for an example.
 */
public interface Remote {

    void configure(Config.RemoteConfig config);

    /**
     * Called after instantiating the Remote to validate the configuration has all information required to use the remote.
     *
     * @throws IllegalStateException The remote configuration is invalid.
     */
    default void validateRemoteConfig() throws IllegalStateException{}

    /**
     * Called for each document to validate that all metadata required by the remote exists.
     *
     * @param metadata
     * @throws IllegalStateException The document metadata is invalid.
     */
    default void validateDocumentMetadata(Config.DocumentMetadata metadata) throws IllegalStateException{}

    /**
     * Called to create a new document on the remote.
     * <p>
     * If the remote doesnt support native markdown, the metadata should contain link to converted html file in
     * {@link com.github.macgregor.alexandria.Context#convertedPaths} by the time this method is called. The
     * implementation is responsible for updating the metadata with:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#createdOn}
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#lastUpdated}
     *  <li>any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     * </ul>
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void create(Context context, Config.DocumentMetadata metadata) throws IOException;

    /**
     * Called to update an existing document on the remote.
     * <p>
     * If the remote doesnt support native markdown, the metadata should contain link to converted html file in
     * {@link com.github.macgregor.alexandria.Context#convertedPaths} by the time this method is called. The implementation
     * is responsible for updating the metadata with:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#lastUpdated}
     *  <li>any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     * </ul>
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void update(Context context, Config.DocumentMetadata metadata) throws IOException;

    /**
     * Called to delete an existing document on the remote.
     * <p>
     * The implementation is responsible for updating the metadata with:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn}
     *  <li>any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     * </ul>
     *
     * @param metadata
     * @throws IOException Errors with local files or any requests made to the remote.
     */
    void delete(Context context, Config.DocumentMetadata metadata) throws IOException;
}
