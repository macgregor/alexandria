package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.HttpException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the Jive rest api for create/update/delete of documents.
 * <p>
 * Does not currently support OAuth as I do not have access to a Jive instance that supports it.
 * <p>
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
 *
 * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#extraProps}:
 * <ul>
 *  <li><b>USER DEFINED</b> {@value #JIVE_PARENT_URI} - the uri used to access a parent place for the document. Setting this is how you link a
 *  Jive document with the place where it will live. This will not be used for the api calls as the api needs a different URI
 *  (of course). This is the link a human can easily find in their browser, we have to search for the place to get the details
 *  needed by the content api.
 *  @see #findParentPlace(Context, Config.DocumentMetadata)
 *  <li>{@value #JIVE_CONTENT_ID} - the identifier Jive uses for a document, hard for the user to get themselves. We either
 *  set it when we create the document or look it up from {@link com.github.macgregor.alexandria.Config.DocumentMetadata#remoteUri}.
 *  @see #findDocument(Context, Config.DocumentMetadata)
 *  <li>{@value #JIVE_PARENT_PLACE_ID} - the identifier Jive uses for a place. Set when we lookup the parent place from the
 *  user defined {@value #JIVE_PARENT_URI}.
 *  @see #findParentPlace(Context, Config.DocumentMetadata)
 *  <li>{@value #JIVE_PARENT_API_URI} - this is the actual api uri for a parent place. Set when we lookup the parent place
 *  from the user defined {@value #JIVE_PARENT_URI}.
 *  @see #findParentPlace(Context, Config.DocumentMetadata)
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
    public static final String STANDARD_FIELD_PROJECTION = "id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent";

    public static final String JIVE_CONTENT_ID = "jiveContentId";
    public static final String JIVE_PARENT_URI = "jiveParentUri";
    public static final String JIVE_PARENT_API_URI = "jiveParentApiUri";
    public static final String JIVE_PARENT_PLACE_ID = "jiveParentPlaceId";

    /**
     * Create {@link JiveRemote} with a default {@link OkHttpClient}.
     *
     * @param config  remote configuration with at least {@link com.github.macgregor.alexandria.Config.RemoteConfig#clazz} set.
     */
    public JiveRemote(Config.RemoteConfig config){
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Config.RemoteConfig config){
        this.client = new OkHttpClient();
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * Requires:
     * <ul>
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#baseUrl}
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#username}
     *  <li>{@link com.github.macgregor.alexandria.Config.RemoteConfig#password}
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
        if(needsParentPlaceUri(metadata)){
            findParentPlace(context, metadata);
        }

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("contents")
                .addQueryParameter("fields", STANDARD_FIELD_PROJECTION)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .post(RequestBody.create(JSON, documentPostBody(context, metadata)))
                .build();

        Response response = doRequest(request);
        try {
            JiveContent jiveContent =  Jackson.jsonMapper().readValue(response.body().charStream(), JiveContent.class);
            updateMetadata(metadata, jiveContent);
        } catch (IOException e) {
            log.warn("Cannot parse response content", e);
            HttpException exception = new HttpException("Cannot parse response content", e);
            exception.request(Optional.of(request));
            exception.response(Optional.ofNullable(response));
            throw exception;
        }
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
        if(needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = metadata.extraProps().get().get(JIVE_CONTENT_ID);

        if(needsParentPlaceUri(metadata)){
            findParentPlace(context, metadata);
        }

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(contentId)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .put(RequestBody.create(JSON, documentPostBody(context, metadata)))
                .build();

        Response response = doRequest(request);
        try {
            JiveContent jiveContent =  Jackson.jsonMapper().readValue(response.body().charStream(), JiveContent.class);
            updateMetadata(metadata, jiveContent);
        } catch (IOException e) {
            log.warn("Cannot parse response content", e);
            throw new HttpException.Builder()
                    .withMessage("Cannot parse response content")
                    .causedBy(e)
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        }
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

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(contentId)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .get()
                .build();

        Response response;
        try{
            response = doRequest(request);
        } catch(HttpException e){
            if(e.response().isPresent() && e.response().get().code() == 404){
                log.debug("Looking for document to delete returned a 404, assuming its already deleted.");
                metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
                if(metadata.hasExtraProperty("delete")){
                    metadata.extraProps().get().remove("delete");
                }
                return;
            }
            throw e;
        }

        request = authenticated(new Request.Builder())
                .url(route)
                .delete()
                .build();

        response = doRequest(request);
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
     *
     * @param metadata  metadata to find on remote
     * @throws IOException  there was a problem with the request
     */
    public void findDocument(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Missing jive content id for %s, attempting to retrieve from remote.", metadata.remoteUri().get().toString()));

        String filter = String.format("entityDescriptor(102,%s)", jiveObjectId(metadata.remoteUri().get()));

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("contents")
                .addQueryParameter("filter", filter)
                .addQueryParameter("startIndex", "0")
                .addQueryParameter("count", "1")
                .addQueryParameter("fields", STANDARD_FIELD_PROJECTION)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .get()
                .build();

        Response response = doRequest(request);
        try {
            PagedJiveContent pagedJiveContent = Jackson.jsonMapper().readValue(response.body().charStream(), PagedJiveContent.class);
            updateMetadata(metadata, pagedJiveContent);
        } catch (IOException e) {
            log.warn("Cannot parse response content", e);
            HttpException exception = new HttpException("Cannot parse response content", e);
            exception.request(Optional.of(request));
            exception.response(Optional.ofNullable(response));
            throw exception;
        }
    }

    /**
     * Determine if an indexed document has a parent ({@value JIVE_PARENT_URI} but needs to have the parent place id looked up.
     *
     * @param metadata  document to check for parent information
     * @return  true if document has a parent but no {@value JIVE_PARENT_API_URI}, false if no parent or {@value JIVE_PARENT_API_URI} already set
     */
    public static boolean needsParentPlaceUri(Config.DocumentMetadata metadata){
        return metadata.extraProps().isPresent() &&
                metadata.extraProps().get().containsKey(JIVE_PARENT_URI) &&
                !metadata.extraProps().get().containsKey(JIVE_PARENT_API_URI);
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

        String parentPlaceUrl = metadata.extraProps().get().get(JIVE_PARENT_URI);
        String filter = String.format("search(%s)", jiveParentPlaceName(parentPlaceUrl));

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("places")
                .addQueryParameter("filter", filter)
                .addQueryParameter("startIndex", "0")
                .addQueryParameter("count", "1")
                .addQueryParameter("fields", JivePlace.FIELDS)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .get()
                .build();

        Response response = doRequest(request);
        try {
            PagedJivePlace pagedJiveContent = Jackson.jsonMapper().readValue(response.body().charStream(), PagedJivePlace.class);
            updateMetadata(metadata, pagedJiveContent);
        } catch (IOException e) {
            log.warn("Cannot parse response content", e);
            HttpException exception = new HttpException("Cannot parse response content", e);
            exception.request(Optional.of(request));
            exception.response(Optional.ofNullable(response));
            throw exception;
        }
    }

    /**
     * Convenience method for adding username and password to request builder.
     *
     * @param builder  request to add credentials to
     * @return  the builder passed to it with credentials added
     */
    public Request.Builder authenticated(Request.Builder builder) {
        builder.addHeader("Authorization", Credentials.basic(config.username().get(), config.password().get()));
        return builder;
    }

    /**
     * Update metadata from the {@link PagedJiveContent} from a {@link #findDocument(Context, Config.DocumentMetadata)} request.
     *
     * @param metadata  document to update with request content
     * @param content  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, PagedJiveContent content) {
        if(content != null && content.list != null && content.list.size() > 0){
            return updateMetadata(metadata, content.list.get(0));
        }

        return metadata;
    }

    /**
     * Update metadata from the {@link JiveContent} from a create, update or find request
     * @param metadata  document to update with request content
     * @param content  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveContent content) {
        metadata.createdOn(Optional.ofNullable(content.published));
        metadata.lastUpdated(Optional.ofNullable(content.updated));

        if(content.resources != null && content.resources.containsKey("html")){
            try {
                metadata.remoteUri(Optional.of(new URI(content.resources.get("html").ref)));
            } catch (Exception e) {}
        }
        if(content.parentPlace != null){
            if(StringUtils.isNotBlank(content.parentPlace.html)){
                metadata.extraProps().get().put(JIVE_PARENT_URI, content.parentPlace.html);
            }
            if(StringUtils.isNotBlank(content.parentPlace.placeID)){
                metadata.extraProps().get().put(JIVE_PARENT_PLACE_ID, content.parentPlace.placeID);
            }
            if(StringUtils.isNotBlank(content.parentPlace.uri)){
                metadata.extraProps().get().put(JIVE_PARENT_API_URI, content.parentPlace.uri);
            }
        }
        if(StringUtils.isNotBlank(content.contentID)){
            metadata.extraProps().get().put(JIVE_CONTENT_ID, content.contentID);
        }

        log.debug(String.format("Updated %s metadata from response content.", metadata.sourcePath().toAbsolutePath().toString()));
        return metadata;
    }

    /**
     * Update metadata from the {@link PagedJivePlace} from a {@link #findParentPlace(Context, Config.DocumentMetadata)} request.
     *
     * @param metadata  document to update with request content
     * @param places  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, PagedJivePlace places) {
        if(places != null && places.list != null && places.list.size() > 0){
            return updateMetadata(metadata, places.list.get(0));
        }

        return metadata;
    }

    /**
     * Update metadata from the {@link JivePlace}
     *
     * @param metadata  document to update with request content
     * @param place  parsed content from the request
     * @return  the updated document metadata passed to it
     */
    protected static Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JivePlace place) {
        if(StringUtils.isNotBlank(place.placeID)){
            metadata.extraProps().get().put(JIVE_PARENT_PLACE_ID, place.placeID);
        }
        if(place.resources != null && place.resources.containsKey("html")){
            metadata.extraProps().get().put(JIVE_PARENT_URI, place.resources.get("html").ref);
        }
        if(place.resources != null && place.resources.containsKey("self")){
            metadata.extraProps().get().put(JIVE_PARENT_API_URI, place.resources.get("self").ref);
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
     * Create the post body for a create or update request from the given {@link com.github.macgregor.alexandria.Config.DocumentMetadata}.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html">Jive REST API - Document Entity</a>
     *
     * @param metadata  document metadata to generate post body from
     * @return  String representation of the json structure for the request
     */
    protected static String documentPostBody(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveContent jiveDocument = new JiveContent();
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

        if(metadata.tags().isPresent()){
            jiveDocument.tags = metadata.tags().get();
        }

        String body = Jackson.jsonMapper().writeValueAsString(jiveDocument);
        log.trace(body);
        return body;
    }

    public static class Link{
        public String ref;
        public List<String> allowed;
    }

    public static class PagedJivePlace{
        public List<JivePlace> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }

    public static class JivePlace{
        public static final String FIELDS = "id,resources,placeID,displayName,name,type,typeCode";
        public Integer id;
        public Map<String, Link> resources = new HashMap<>();
        public String placeID;
        public String displayName;
        public String name;
        public String type;
        public Integer typeCode;
    }

    public static class PagedJiveContent{
        public Map<String, String> links = new HashMap<>();
        public List<JiveContent> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }

    public static class JiveContent{
        public Integer id;
        public String contentID;
        public ZonedDateTime published;
        public ZonedDateTime updated;
        public List<String> tags = new ArrayList<>();
        public String type; // "document" for documents
        public Integer typeCode; // 102 for documents
        public String subject; //document name
        public Content content = new Content();
        public Via via = new Via();
        public Map<String, Link> resources = new HashMap<>();
        public String parent;
        public ParentPlace parentPlace;

        public JiveContent(){}

        public static class Content{
            public String type;
            public String text;
            public Boolean editable;
        }

        public static class Via{
            public final String displayName = "Alexandria";
            public final String url = "https://github.com/macgregor/alexandria";
        }

        public static class ParentPlace{
            public Integer id;
            public String html;
            public String placeID;
            public String name;
            public String type;
            public String uri;
        }
    }
}
