package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.HttpException;
import com.github.macgregor.alexandria.remotes.jive.JiveData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *   class: "com.github.macgregor.alexandria.remotes.JiveRemote"
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
 * }
 * </pre>
 *
 * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}:
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
public class JiveRemote extends RestRemote implements Remote{

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final String JIVE_CONTENT_ID = "jiveContentId";
    public static final String JIVE_PARENT_URI = "jiveParentUri";
    public static final String JIVE_PARENT_API_URI = "jiveParentApiUri";
    public static final String JIVE_PARENT_PLACE_ID = "jiveParentPlaceId";
    public static final String JIVE_TRACKING_TAG = "jiveTrackingTag";

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
     * @see #needsParentPlaceUri(Config.DocumentMetadata)
     * @see #findParentPlace(Context, Config.DocumentMetadata)
     */
    @Override
    public void create(Context context, Config.DocumentMetadata metadata) throws IOException {
        setTrackingTagAsNeeded(context, metadata);
        try {
            findDocument(context, metadata);
        } catch(HttpException e){
            if(e.response().isPresent() && e.response().get().code() == 404){
                log.debug(String.format("Document %s not found on remote. Creating.", metadata.sourceFileName()));
            } else{
                throw e;
            }
        }

        if(needsParentPlaceUri(metadata)){
            findParentPlace(context, metadata);
        }

        RemoteDocument<JiveData.JiveContent> jiveContent = RemoteDocument.<JiveData.JiveContent>builder()
                .baseUrl(config.baseUrl().get())
                .pathSegment("contents")
                .entity(JiveData.JiveContent.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("fields", JiveData.JiveContent.FIELDS)
                .build();
        JiveData.JiveContent content = jiveContent.post(documentPostBody(context, metadata));
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
     * @see #needsContentId(Config.DocumentMetadata)
     * @see #findDocument(Context, Config.DocumentMetadata)
     */
    @Override
    public void update(Context context, Config.DocumentMetadata metadata) throws IOException {
        setTrackingTagAsNeeded(context, metadata);
        if(needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = metadata.extraProps().get().get(JIVE_CONTENT_ID);

        if(needsParentPlaceUri(metadata)){
            findParentPlace(context, metadata);
        }

        RemoteDocument<JiveData.JiveContent> jiveContent = RemoteDocument.<JiveData.JiveContent>builder()
                .baseUrl(config.baseUrl().get())
                .pathSegment("contents")
                .pathSegment(contentId)
                .entity(JiveData.JiveContent.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("fields", JiveData.JiveContent.FIELDS)
                .build();
        JiveData.JiveContent content = jiveContent.put(documentPostBody(context, metadata));
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
     * @see #needsContentId(Config.DocumentMetadata)
     * @see #findDocument(Context, Config.DocumentMetadata)
     */
    @Override
    public void delete(Context context, Config.DocumentMetadata metadata) throws IOException {
        // edge case here where the content is deleted on the remote, and doesnt have a jive content id in the metadata index
        // we will get a 404 here and fail.
        if(needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = metadata.extraProps().get().get(JIVE_CONTENT_ID);

        RemoteDocument<JiveData.JiveContent> jiveContent = RemoteDocument.<JiveData.JiveContent>builder()
                .baseUrl(config.baseUrl().get())
                .pathSegment("contents")
                .pathSegment(contentId)
                .entity(JiveData.JiveContent.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("fields", JiveData.JiveContent.FIELDS)
                .build();

        try{
            jiveContent.get();
        } catch(HttpException e){
            if(e.response().isPresent() && e.response().get().code() == 404){
                log.debug("Looking for document to delete returned a 404, assuming its already deleted.");
                metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
                return;
            }
            throw e;
        }

        jiveContent.delete();
        metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
        if(metadata.hasExtraProperty("delete")){
            metadata.extraProps().get().remove("delete");
        }
    }

    /**
     * Determine if the indexed document needs to fetch the {@value #JIVE_CONTENT_ID} from the remote.
     *
     * @param metadata  document to check
     * @return  true if {@value JIVE_CONTENT_ID} needs to be retrieved from remote, false if its already set.
     */
    protected static boolean needsContentId(Config.DocumentMetadata metadata){
        if(metadata.remoteUri().isPresent()){
            if(metadata.extraProps().isPresent()){
                return !metadata.extraProps().get().containsKey(JIVE_CONTENT_ID);
            }
            return true;
        }
        return false;
    }

    /**
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
     */
    public void findDocument(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Missing jive content id for %s, attempting to retrieve from remote.", metadata.sourceFileName()));

        String filter;
        if(metadata.hasExtraProperty(JIVE_TRACKING_TAG)){
            filter = String.format("tag(%s)", metadata.getExtraProperty(JIVE_TRACKING_TAG));
        } else if (metadata.remoteUri().isPresent()){
            filter = String.format("entityDescriptor(102,%s)", jiveObjectId(metadata.remoteUri().get()));
        } else{
            throw new AlexandriaException.Builder()
                    .withMessage("Not enough information to find document on remote. Manual intervention may be necessary.")
                    .metadataContext(metadata)
                    .build();
        }

        RemoteDocument<JiveData.PagedJiveContent> pagedJiveContent = RemoteDocument.<JiveData.PagedJiveContent>builder()
                .baseUrl(config.baseUrl().get())
                .entity(JiveData.PagedJiveContent.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("filter", filter)
                .queryParameter("startIndex", "0")
                .queryParameter("count", "1")
                .queryParameter("fields", JiveData.JiveContent.FIELDS)
                .build();
        JiveData.PagedJiveContent content = pagedJiveContent.get();
        updateMetadata(metadata, content);
    }

    /**
     * Determine if an indexed document has a parent ({@value JIVE_PARENT_URI} but needs to have the parent place id looked up.
     *
     * @param metadata  document to check for parent information
     * @return  true if document has a parent but no {@value JIVE_PARENT_API_URI}, false if no parent or {@value JIVE_PARENT_API_URI} already set
     */
    public static boolean needsParentPlaceUri(Config.DocumentMetadata metadata){
        return metadata.hasExtraProperty(JIVE_PARENT_URI) && !metadata.hasExtraProperty(JIVE_PARENT_API_URI);
    }

    /**
     * Just like with documents, the parent uri a human interacts with is not the same as the one we need for rest requests.
     * This gives us the {@value JIVE_PARENT_API_URI} to use in the post body of create and update requests.
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

        String parentPlaceUrl = metadata.getExtraProperty(JIVE_PARENT_URI);
        String filter = String.format("search(%s)", jiveParentPlaceName(parentPlaceUrl));

        RemoteDocument<JiveData.PagedJivePlace> pagedJivePlace = RemoteDocument.<JiveData.PagedJivePlace>builder()
                .baseUrl(config.baseUrl().get())
                .entity(JiveData.PagedJivePlace.class)
                .header("Authorization", Credentials.basic(config.username().get(), config.password().get()))
                .queryParameter("filter", filter)
                .queryParameter("startIndex", "0")
                .queryParameter("count", "1")
                .queryParameter("fields", JiveData.JivePlace.FIELDS)
                .build();
        JiveData.PagedJivePlace content = pagedJivePlace.get();
        updateMetadata(metadata, content);
    }

    /**
     * Update metadata from the {@link JiveData.PagedJiveContent} from a {@link #findDocument(Context, Config.DocumentMetadata)} request.
     *
     * @param metadata  document to update with request content
     * @param content  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveData.PagedJiveContent content) {
        if(content != null && content.list != null && content.list.size() > 0){
            return updateMetadata(metadata, content.list.get(0));
        }

        return metadata;
    }

    /**
     * Update metadata from the {@link JiveData.JiveContent} from a create, update or find request
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
     * Update metadata from the {@link JiveData.PagedJivePlace} from a {@link #findParentPlace(Context, Config.DocumentMetadata)} request.
     *
     * @param metadata  document to update with request content
     * @param places  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveData.PagedJivePlace places) {
        if(places != null && places.list != null && places.list.size() > 0){
            return updateMetadata(metadata, places.list.get(0));
        }

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
     * Extract the pseudo-identifier used in the {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * for use in a search request to retrieve the actual {@value JIVE_CONTENT_ID}.
     *
     * @param remoteDoc  {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}
     * @return  jive object id extracted from the uri
     */
    protected static String jiveObjectId(URI remoteDoc){
        Pattern p = Pattern.compile(".*DOC-(\\d+)-*.*");
        Matcher m = p.matcher(remoteDoc.getPath());
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to extract jive object id from %s.", remoteDoc.toString()));
        }
    }

    /**
     * Extract the parent place name from the user defined {@value JIVE_PARENT_URI} for use in a search request for the
     * actual {@value JIVE_PARENT_API_URI}.
     *
     * @param parentPlaceUrl  {@value #JIVE_PARENT_URI} value from {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}
     * @return  the extracted parent place name
     */
    protected static String jiveParentPlaceName(String parentPlaceUrl){
        Pattern p = Pattern.compile(".*/(.*)");
        Matcher m = p.matcher(parentPlaceUrl);
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to parent place name from %s.", parentPlaceUrl));
        }
    }

    /**
     * Set a UUID tracking tag Alexandria can use to track down documents.
     *
     * Rest APIs are fickle things. They may or may not provide decent search apis or reliable methods
     * that return data needed to identify created or updated documents. Adding this tag lets us quickly
     * and easily search for documents if we cant use the Jive identifier for some reason.
     *
     * @param context  current Alexandria context
     * @param metadata  document metadata to add tracking tag to
     */
    protected static void setTrackingTagAsNeeded(Context context, Config.DocumentMetadata metadata){
        if(metadata.hasExtraProperty(JIVE_TRACKING_TAG)){
            return;
        }
        metadata.setExtraProperty(JIVE_TRACKING_TAG, UUID.randomUUID().toString());
    }

    /**
     * Set tags for the document resolving default tags, document tags and remote specific tags.
     *
     * @param context  Current Alexandria context
     * @param metadata  document metadata to get tags for
     * @return  list of tags to add to the request or empty list if none are set
     */
    protected static List<String> getTagsForDocument(Context context, Config.DocumentMetadata metadata){
        List<String> tags = new ArrayList();
        if(context.config().defaultTags().isPresent()){
            tags.addAll(context.config().defaultTags().get());
        }
        if(metadata.tags().isPresent()){
            tags.addAll(metadata.tags().get());
        }
        if(metadata.hasExtraProperty(JIVE_TRACKING_TAG)) {
            tags.add(metadata.getExtraProperty(JIVE_TRACKING_TAG));
        }
        return tags;
    }

    /**
     * Create the post body for a create or update request from the given {@link com.github.macgregor.alexandria.Config.DocumentMetadata}.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html">Jive REST API - Document Entity</a>
     *
     * @param context  current Alexandria context
     * @param metadata  document metadata to generate post body from
     * @return  String representation of the json structure for the request
     * @throws IOException  the converted document couldnt be loaded
     */
    protected static JiveData.JiveContent documentPostBody(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveData.JiveContent jiveDocument = new JiveData.JiveContent();
        jiveDocument.parentPlace = null; //parent place is only in responses
        jiveDocument.subject = metadata.title();
        jiveDocument.content.text = Resources.load(context.convertedPath(metadata).get().toString());
        jiveDocument.type = "document";
        jiveDocument.typeCode = 102;

        if(metadata.extraProps().get().containsKey(JIVE_CONTENT_ID)){
            jiveDocument.contentID = metadata.extraProps().get().get(JIVE_CONTENT_ID);
        }

        if(metadata.extraProps().get().containsKey(JIVE_PARENT_API_URI)) {
            jiveDocument.parent = metadata.extraProps().get().get(JIVE_PARENT_API_URI);
        }

        jiveDocument.tags = getTagsForDocument(context, metadata);
        return jiveDocument;
    }
}
