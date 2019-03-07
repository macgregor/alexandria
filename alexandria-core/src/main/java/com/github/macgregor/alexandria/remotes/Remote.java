package com.github.macgregor.alexandria.remotes;


import com.github.macgregor.alexandria.AlexandriaSync;
import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;

import java.io.IOException;

/**
 * Defines an interface for interacting with a remote document source.
 *
 * If a {@link Remote} needs the Alexandria {@link com.github.macgregor.alexandria.Context} for its work, it can
 * also implement the {@link com.github.macgregor.alexandria.Context.ContextAware} interface and the {@link com.github.macgregor.alexandria.Context}
 * will be injected when Alexandria instantiates the {@link Remote}
 *
 * @see {@link com.github.macgregor.alexandria.Context.ContextAware}
 * @see {@link NoopRemote}
 */
public interface Remote {

    /**
     * Called after instantiating the {@link Remote} and before {@link Remote#validateRemoteConfig()} so that the
     * implementation can configure itself.
     * @param config
     */
    void configure(Config.RemoteConfig config);

    /**
     * Called after instantiating the {@link Remote} and configuring it with {@link Remote#validateRemoteConfig()} to
     * validate the configuration has all information required to use the remote.
     *
     * @throws IllegalStateException  The remote configuration is invalid.
     */
    default void validateRemoteConfig() throws IllegalStateException {}

    /**
     * Called for each document to validate that all metadata required by the remote exists.
     *
     * @param metadata  document to validate
     * @throws IllegalStateException  The document metadata is invalid.
     */
    default void validateDocumentMetadata(Config.DocumentMetadata metadata) throws IllegalStateException {}

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
     * @param metadata  document to create
     * @throws IOException  Errors with local files or any requests made to the remote.
     */
    void create(Config.DocumentMetadata metadata) throws IOException;

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
     * @param metadata  document to update
     * @throws IOException  Errors with local files or any requests made to the remote.
     */
    void update(Config.DocumentMetadata metadata) throws IOException;

    /**
     * Called to delete an existing document on the remote.
     * <p>
     * The implementation is responsible for updating the metadata with:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.DocumentMetadata#deletedOn}
     *  <li>any remote specific metadata in {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     * </ul>
     *
     * @param metadata  document to delete
     * @throws IOException  Errors with local files or any requests made to the remote.
     */
    void delete(Config.DocumentMetadata metadata) throws IOException;

    /**
     * Retrieve the {@link Remote} {@link MarkdownConverter} implementation.
     *
     * @return
     */
    MarkdownConverter markdownConverter();

    /**
     * Called after instantiating the {@link Remote} to provide the remote with the {@link MarkdownConverter}
     * implementation configured in {@link com.github.macgregor.alexandria.Config.RemoteConfig#converterClazz}
     *
     * @param markdownConverter
     */
    void markdownConverter(MarkdownConverter markdownConverter);

    /**
     * Tells the {@link AlexandriaSync#syncWithRemote()} process whether two passes are necessary to properly keep
     * documents up to date. Defaults to true.
     *
     * This is needed when a {@link Remote} relies on state that may change after a sync, such as resolving relative
     * links into a remote URI for newly created documents. Users would need to run sync once to create the documents,
     * then a second time so that the {@link MarkdownConverter} can properly resolve the file system links to the documents
     * that were just created.
     *
     * Switch to false if you know you dont need it and efficiencies is a concern.
     *
     * @return true if {@link AlexandriaSync#syncWithRemote()} should always run twice for a remote, or false to run once
     */
    default boolean twoPassSync(){
        return true;
    }
}
