package com.github.macgregor.alexandria.remotes.jive;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.HttpException;
import com.github.macgregor.alexandria.remotes.Remote;
import com.github.macgregor.alexandria.remotes.RemoteDocument;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implements the Jive rest api for create/update/delete of documents.
 *
 * Does not currently support OAuth as I do not have access to a Jive instance that supports it.
 * <pre>
 * {@code
 * ---
 * remote:
 *   baseUrl: "https://jive.com/api/core/v3"
 *   username: "username"
 *   password: "password"
 *   supportsNativeMarkdown: false
 *   datetimeFormat: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
 *   requestTimeout: 60
 *   class: "com.github.macgregor.alexandria.remotes.jive.JiveRemote"
 *   defaultExtraProps:
 *      jiveParentUri: "https://jive.com/groups/alexandria-test-group"
 * metadata:
 * - sourcePath: "docs/images.md"
 *   title: "images.md"
 *   remoteUri: "https://jive.com/docs/DOC-1140818"
 *   sourceChecksum: 1751689934
 *   convertedChecksum: 2834756773
 *   createdOn: "2018-08-22T02:27:48.644+0000"
 *   lastUpdated: "2018-08-22T02:27:51.789+0000"
 *   extraProps:
 *     jiveParentUri: "https://jive.com/groups/alexandria-test-group"
 *     jiveParentPlaceId: "1448512"
 *     jiveParentApiUri: "https://jive.com/api/core/v3/places/1448512"
 *     jiveContentId: "1448517"
 *     jiveTrackingTag: "fb25f4ee-b084-4bb2-a96f-3e656449ca20"
 * }
 * </pre>
 *
 * {@link com.github.macgregor.alexandria.Config.RemoteConfig#defaultExtraProps} and {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}:
 * <ul>
 *  <li><b>USER DEFINED</b> {@value #JIVE_PARENT_URI} - the uri used to access a parent place for the document. Setting this is how you link a
 *  Jive document with the place where it will live. This will not be used for the api calls as the api needs a different URI
 *  (of course). This is the link a human can easily find in their browser, we have to search for the place to get the details
 *  needed by the content api.
 *  See {@link #findParentPlace(Context, Config.DocumentMetadata)}</li>
 *  <li>{@value #JIVE_CONTENT_ID} - the identifier Jive uses for a document, hard for the user to get themselves. We either
 *  set it when we create the document or look it up from {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}.
 *  See {@link #findDocument(Context, Config.DocumentMetadata)}</li>
 *  <li>{@value #JIVE_PARENT_PLACE_ID} - the identifier Jive uses for a place. Set when we lookup the parent place from the
 *  user defined {@value #JIVE_PARENT_URI}.
 *  See {@link #findParentPlace(Context, Config.DocumentMetadata)}</li>
 *  <li>{@value #JIVE_PARENT_API_URI} - this is the actual api uri for a parent place. Set when we lookup the parent place
 *  from the user defined {@value #JIVE_PARENT_URI}.
 *  See {@link #findParentPlace(Context, Config.DocumentMetadata)}</li>
 *  <li>{@value #JIVE_TRACKING_TAG} - this is a tag set to help Alexandria track documents created or updated in case it needs
 *  to find them later. Poor performance on the Jive instance can easily lead to state like the create request timing out
 *  but still going through server side. We need to be able to locate documents easily without knowing the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
 *  or {@value JIVE_CONTENT_ID}</li>
 * </ul>
 *
 * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/index.html">Introduction to the Jive REST API</a>
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor
public class JiveRemote implements Remote {
    public static final String JIVE_CONTENT_ID = "jiveContentId";
    public static final String JIVE_PARENT_URI = "jiveParentUri";
    public static final String JIVE_PARENT_API_URI = "jiveParentApiUri";
    public static final String JIVE_PARENT_PLACE_ID = "jiveParentPlaceId";
    public static final String JIVE_TRACKING_TAG = "jiveTrackingTag";

    @NonNull protected OkHttpClient client;
    @NonNull protected Config.RemoteConfig config;

    /**
     * Create {@link JiveRemote} with a default {@link OkHttpClient}.
     *
     * @param config  remote configuration with at least {@link com.github.macgregor.alexandria.Config.RemoteConfig#clazz} set.
     */
    public JiveRemote(Config.RemoteConfig config){
        client = new OkHttpClient.Builder()
                .connectTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .build();
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Config.RemoteConfig config){
        this.client = new OkHttpClient.Builder()
                .connectTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.requestTimeout(), TimeUnit.SECONDS)
                .build();
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * Requires:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#baseUrl}</li>
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#username}</li>
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#password}</li>
     * </ul>
     *
     * @throws IllegalStateException  Username, password and/or baseUrl are not set.
     */
    @Override
    public void validateRemoteConfig() throws IllegalStateException {
        List<String> missingProperties = new ArrayList<>();
        if(!config.baseUrl().isPresent()){
            missingProperties.add("remote.baseUrl");
        }
        if(!config.username().isPresent()){
            missingProperties.add("remote.username");
        }
        if(!config.password().isPresent()){
            missingProperties.add("remote.password");
        }
        if(!missingProperties.isEmpty()){
            log.warn(String.format("Jive remote configuration missing required properties: %s", missingProperties));
            throw new IllegalStateException(String.format("Jive remote configuration missing required properties: %s", missingProperties));
        }
    }

    /**
     * {@inheritDoc}
     *
     * We will always make a request to {@code POST baseUrl/contents}, but we may also need to make a request to
     * {@code GET baseUrl/places} if the document has a parent place.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#createContent(String,%20String,%20String,%20String)">Jive REST API - Create Content</a>
     * @see JiveUtils#needsParentPlaceUri(Context, Config.DocumentMetadata)
     * @see #findParentPlace(Context, Config.DocumentMetadata)
     */
    @Override
    public void create(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveUtils.setTrackingTagAsNeeded(context, metadata);
        boolean found = false;
        try {
            JiveData.JiveContent content = findDocument(context, metadata);
            if(content != null){
                found = true;
            }
        } catch(HttpException e){
            if(e.response().isPresent() && e.response().get().code() == 404){
                log.debug(String.format("Document %s not found on remote. Creating.", metadata.sourceFileName()));
            } else {
                throw e;
            }
        }

        if(found){
            log.debug(String.format("Document %s (%s) already exists on remote. Not recreating.", metadata.sourceFileName(), metadata.remoteUri().get()));
            return;
        }

        if(JiveUtils.needsParentPlaceUri(context, metadata)){
            findParentPlace(context, metadata);
        }

        RemoteDocument<JiveData.JiveContent> jiveContent = remoteJiveContentBuilder()
                .build();
        JiveData.JiveContent content = jiveContent.post(JiveData.documentPostBody(context, metadata));
        updateMetadata(metadata, content);
    }

    /**
     * {@inheritDoc}
     *
     * We will always make a request to {@code POST baseUrl/contents/$&#123;{@value #JIVE_CONTENT_ID}&#125;}, but
     * we may also need to make a request to {@code GET baseUrl/contents} to convert the browsers reachable document
     * endpoint to the rest api endpoint for the document.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#updateContent(String,%20String,%20String,%20boolean,%20String,%20boolean)">Jive REST API - Update Content</a>
     * @see JiveUtils#needsContentId(Config.DocumentMetadata)
     * @see #findDocument(Context, Config.DocumentMetadata)
     */
    @Override
    public void update(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveUtils.setTrackingTagAsNeeded(context, metadata);
        if(JiveUtils.needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = context.getExtraPropertiesForDocument(metadata).get(JIVE_CONTENT_ID);

        if(JiveUtils.needsParentPlaceUri(context, metadata)){
            findParentPlace(context, metadata);
        }

        RemoteDocument<JiveData.JiveContent> jiveContent = remoteJiveContentBuilder()
                .pathSegment(contentId)
                .build();
        JiveData.JiveContent content = jiveContent.put(JiveData.documentPostBody(context, metadata));
        updateMetadata(metadata, content);
    }
    /**
     * {@inheritDoc}
     *
     * Potentially three requests can be made here, because the Jive api handles requests for deleted documents poorly.
     * Firstly, the usual {@code GET baseUrl/contents} if the metadata is missing the {@value #JIVE_CONTENT_ID}. After that
     * attempts to delete a document that is already deleted will yield a 403 unauthorized. So if for some reason
     * Alexandria's state gets messed up and it tries to re-delete a document it will get an unexpected error. We avoid
     * that by fetching the document directly via {@code GET baseUrl/contents/$&#123;{@value #JIVE_CONTENT_ID}&#125;},
     * checking for 403's and only executing {@code DELETE baseUrl/contents/$&#123;{@value #JIVE_CONTENT_ID}&#125;} if
     * the document exists. Then we know a 403 is actually an authorization error.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#getContent(String,%20String,%20boolean,%20List%3CString%3E)">Jive REST API - Get Content</a>
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#deleteContent(String,%20Boolean)">Jive REST API - Delete Content</a>
     * @see JiveUtils#needsContentId(Config.DocumentMetadata)
     * @see #findDocument(Context, Config.DocumentMetadata)
     */
    @Override
    public void delete(Context context, Config.DocumentMetadata metadata) throws IOException {
        boolean deleted = false;
        try{
            JiveData.JiveContent content = findDocument(context, metadata);
            if(content == null){
                deleted = true;
            }
        } catch(HttpException e){
            if(e.response().isPresent() && e.response().get().code() == 404){
                deleted = true;
            } else {
                throw e;
            }
        }

        if(deleted){
            log.debug("Looking for document wasnt found, assuming its already deleted.");
            metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
            return;
        }

        String contentId = context.getExtraPropertiesForDocument(metadata).get(JIVE_CONTENT_ID);
        remoteJiveContentBuilder()
                .pathSegment(contentId)
                .build()
                .delete();
        metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    /**
     * Find a document's api identifiers from the human accessible uri.
     *
     * The uri a human uses to access a document ({@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri})
     * is not the uri we need to make rest requests. There is an sort of identifier in this uri, but we have to extract it
     * and then run a search for it to get the {@value JIVE_CONTENT_ID} which we can use to modify the document.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#getContents(List%3CString%3E,%20String,%20int,%20int,%20String,%20boolean,%20boolean)">Jive REST API - Get Contents</a>
     * @see <a href="https://community.jivesoftware.com/docs/DOC-153931">Finding the Content ID and Place ID using Jive v3 API</a>
     *
     * @param context  current Alexandria context
     * @param metadata  metadata to find on remote
     * @throws IOException  there was a problem with the request
     * @return the matching {@link JiveData.JiveContent} or null if it wasnt found
     */
    public JiveData.JiveContent findDocument(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Missing jive content id for %s, attempting to retrieve from remote.", metadata.sourceFileName()));

        String filter;
        if(metadata.hasExtraProperty(JIVE_TRACKING_TAG)){
            filter = String.format("tag(%s)", context.getExtraPropertiesForDocument(metadata).get(JIVE_TRACKING_TAG));
        } else if (metadata.remoteUri().isPresent()){
            filter = String.format("entityDescriptor(102,%s)", JiveUtils.jiveObjectId(metadata.remoteUri().get()));
        } else{
            throw new AlexandriaException.Builder()
                    .withMessage("Not enough information to find document on remote. Manual intervention may be necessary.")
                    .metadataContext(metadata)
                    .build();
        }

        RemoteDocument<JiveData.JiveContent> pagedJiveContent = remoteJiveContentBuilder()
                .queryParameter("filter", filter)
                .build();
        JiveData.JiveContent content = pagedJiveContent.getPaged().first();
        if(content != null) {
            updateMetadata(metadata, content);
        }
        return content;
    }

    /**
     * Find a parent place's api identifiers from the human accessible uri.
     *
     * Just like with documents, the parent uri a human interacts with is not the same as the one we need for rest requests.
     * This gives us the {@value JIVE_PARENT_API_URI} to use in the post body of create and update requests. The api
     * we are calling for this is very limiting and inaccurate. To compensate for this we run a series of queries of decreasing
     * accuracy:
     * <ol>
     *     <li>api/core/v3/places?filter=relationship(member)</li>
     *     <li>api/core/v3/places?filter=relationship(following)</li>
     *     <li>api/core/v3/places?filter=relationship(owner)</li>
     *     <li>api/core/v3/places?filter=search({@link JiveUtils#jiveParentPlaceName(String)})</li>
     * </ol>
     *
     * All of these require client side filtering to match the place name to the name extracted from the uri. The last query
     * really is a last resort as it is slow and is a coin toss on whether you will find the place or not. For best results,
     * the Jive user Alexandria uses should be made a member, owner or follower of the places it will be using.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/PlaceService.html#getPlaces(List%3CString%3E,%20String,%20int,%20int,%20String)">Jive REST API - Get Places</a>
     * @see <a href="https://community.jivesoftware.com/docs/DOC-153931">Finding the Content ID and Place ID using Jive v3 API</a>
     *
     * @param context  current Alexandria context
     * @param metadata  document that needs parent details
     * @throws IOException  there was a problem with the request
     */
    public void findParentPlace(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Jive parent place detected, attempting to retrieve from remote."));

        String parentPlaceUrl = context.getExtraPropertiesForDocument(metadata).get(JIVE_PARENT_URI);
        String parentPlaceName = JiveUtils.jiveParentPlaceName(parentPlaceUrl);
        List<String> filters = Arrays.asList("relationship(member)", "relationship(following)", "relationship(owner)",
                String.format("search(%s)", parentPlaceName));

        for(String filter : filters) {

            RemoteDocument<JiveData.JivePlace> jivePlaces = remoteJivePlaceBuilder()
                    .queryParameter("filter", filter)
                    .build();
            try {
                for (JiveData.JivePlace place : jivePlaces.getPaged()) {
                    if (place.displayName.equals(parentPlaceName)) {
                        updateMetadata(metadata, place);
                        break;
                    }
                }
                if(!JiveUtils.needsParentPlaceUri(context, metadata)){
                    break;
                }
            } catch (Exception e) {
                if (e.getCause() instanceof HttpException) {
                    HttpException exception = (HttpException) e.getCause();
                    if (!exception.response().isPresent() || exception.response().get().code() != 404) {
                        throw exception;
                    }
                } else {
                    throw e;
                }
            }
        }

        if(JiveUtils.needsParentPlaceUri(context, metadata)){
            log.warn(String.format("Parent Place %s (%s) not found. Document will not be part of any Jive place.", parentPlaceName, parentPlaceUrl));
        }
    }

    /**
     * Update metadata from the {@link JiveData.JiveContent} from a create, update or find request
     *
     * @param metadata  document to update with request content
     * @param content  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveData.JiveContent content) {
        metadata.createdOn(Optional.ofNullable(content.published));
        metadata.lastUpdated(Optional.ofNullable(content.updated));

        if(content.resources != null && content.resources.containsKey("html")){
            try {
                metadata.remoteUri(Optional.of(new URI(content.resources.get("html").ref)));
            } catch (Exception e) {}
        }
        if(content.parentPlace != null){
            if(StringUtils.isNotBlank(content.parentPlace.html)){
                metadata.setExtraProperty(JIVE_PARENT_URI, content.parentPlace.html);
            }
            if(StringUtils.isNotBlank(content.parentPlace.placeID)){
                metadata.setExtraProperty(JIVE_PARENT_PLACE_ID, content.parentPlace.placeID);
            }
            if(StringUtils.isNotBlank(content.parentPlace.uri)){
                metadata.setExtraProperty(JIVE_PARENT_API_URI, content.parentPlace.uri);
            }
        }
        if(StringUtils.isNotBlank(content.contentID)){
            metadata.setExtraProperty(JIVE_CONTENT_ID, content.contentID);
        }

        log.debug(String.format("Updated %s metadata from response content.", metadata.sourcePath().toAbsolutePath().toString()));
        return metadata;
    }

    /**
     * Update metadata from the {@link JiveData.JivePlace}
     *
     * @param metadata  document to update with request content
     * @param place  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveData.JivePlace place) {
        if(StringUtils.isNotBlank(place.placeID)){
            metadata.setExtraProperty(JIVE_PARENT_PLACE_ID, place.placeID);
        }
        if(place.resources != null && place.resources.containsKey("html")){
            metadata.setExtraProperty(JIVE_PARENT_URI, place.resources.get("html").ref);
        }
        if(place.resources != null && place.resources.containsKey("self")){
            metadata.setExtraProperty(JIVE_PARENT_API_URI, place.resources.get("self").ref);
        }

        log.debug(String.format("Updated %s metadata from response content.", metadata.sourcePath().toAbsolutePath().toString()));
        return metadata;
    }

    /**
     * Base {@link RemoteDocument.RemoteDocumentBuilder} for jive content requests.
     *
     * @return  {@link RemoteDocument.RemoteDocumentBuilder} with authorization credentials and field projects set.
     */
    protected RemoteDocument.RemoteDocumentBuilder remoteJivePlaceBuilder(){
        return RemoteDocument.<JiveData.JivePlace>builder()
                .baseUrl(config.baseUrl().get())
                .pathSegment("places")
                .entity(JiveData.JivePlace.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("fields", JiveData.JivePlace.FIELDS);
    }

    /**
     * Base {@link RemoteDocument.RemoteDocumentBuilder} for jive place requests.
     *
     * @return  {@link RemoteDocument.RemoteDocumentBuilder} with authorization credentials and field projects set.
     */
    protected RemoteDocument.RemoteDocumentBuilder remoteJiveContentBuilder(){
        return RemoteDocument.<JiveData.JiveContent>builder()
                .baseUrl(config.baseUrl().get())
                .pathSegment("contents")
                .entity(JiveData.JiveContent.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("fields", JiveData.JiveContent.FIELDS);
    }
}
