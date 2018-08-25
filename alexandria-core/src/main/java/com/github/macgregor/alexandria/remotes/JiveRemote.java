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

@Slf4j
@ToString
@Getter @Setter
@Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class JiveRemote implements Remote{

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String STANDARD_FIELD_PROJECTION = "id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent";

    @NonNull protected OkHttpClient client;
    @NonNull protected Config.RemoteConfig config;

    public JiveRemote(Config.RemoteConfig config){
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.config = config;
    }

    @Override
    public void configure(Config.RemoteConfig config){
        this.client = new OkHttpClient();
        this.config = config;
    }

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
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#createContent(String,%20String,%20String,%20String)
     *
     * @param metadata
     * @throws IOException
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
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#updateContent(String,%20String,%20String,%20boolean,%20String,%20boolean)
     *
     * @param metadata
     * @throws IOException
     */
    @Override
    public void update(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = metadata.extraProps().get().get("jiveContentId");

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
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#deleteContent(String,%20Boolean)
     *
     * @param metadata
     * @throws IOException
     */
    @Override
    public void delete(Context context, Config.DocumentMetadata metadata) throws IOException {
        if(needsContentId(metadata)){
            findDocument(context, metadata);
        }
        String contentId = metadata.extraProps().get().get("jiveContentId");

        HttpUrl route = HttpUrl.parse(config.baseUrl().get()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(contentId)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .delete()
                .build();

        Response response = doRequest(request);
        metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    protected Response doRequest(Request request) throws HttpException {
        log.trace(request.toString());
        Call call = client.newCall(request);

        Response response = null;
        try {
            response = call.execute();
            log.trace(response.toString());
        } catch(IOException e){
            throw new HttpException.Builder()
                    .withMessage(String.format("Unable to make request %s", request.url().toString()))
                    .causedBy(e)
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        }

        if(response.isSuccessful()){
            return response;
        } else if(response.code() == 400){
            throw new HttpException.Builder()
                    .withMessage("Bad request.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if(response.code() == 403){
            throw new HttpException.Builder()
                    .withMessage("Unauthorized to access document.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if(response.code() == 404){
            throw new HttpException.Builder()
                    .withMessage("Document doesnt exist.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if(response.code() == 409){
            throw new HttpException.Builder()
                    .withMessage("Document conflicts with existing document.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else {
            throw new HttpException.Builder()
                    .withMessage(String.format("Unexpected status code %d.", response.code()))
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        }
    }

    public boolean needsContentId(Config.DocumentMetadata metadata){
        return metadata.remoteUri().isPresent() &&
                metadata.extraProps().isPresent() &&
                !metadata.extraProps().get().containsKey("jiveContentId");
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#getContents(List%3CString%3E,%20String,%20int,%20int,%20String,%20boolean,%20boolean)
     *
     * @param metadata
     * @throws IOException
     */
    public void findDocument(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Missing jive content id for %s, attempting to retrieve from remote.", metadata.remoteUri().get().toString()));

        // https://community.jivesoftware.com/docs/DOC-153931
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

    public boolean needsParentPlaceUri(Config.DocumentMetadata metadata){
        return metadata.extraProps().isPresent() &&
                metadata.extraProps().get().containsKey("jiveParentUri") &&
                !metadata.extraProps().get().containsKey("jiveParentApiUri");
    }

    // curl -u sa_es_alexandria:HvvstY3vgiE= "https://mojo-stage.jiveon.com/api/core/v3/places?filter=search(alexandria-test-group)&fields=id,resources,placeID,displayName,name,type,typeCode&startIndex=0&count=1
    public void findParentPlace(Context context, Config.DocumentMetadata metadata) throws IOException {
        log.debug(String.format("Jive parent place detected, attempting to retrieve from remote."));

        String parentPlaceUrl = metadata.extraProps().get().get("jiveParentUri");
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

    public Request.Builder authenticated(Request.Builder builder) {
        builder.addHeader("Authorization", Credentials.basic(config.username().get(), config.password().get()));
        return builder;
    }

    protected Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, PagedJiveContent content) {
        if(content != null && content.list != null && content.list.size() > 0){
            return updateMetadata(metadata, content.list.get(0));
        }

        return metadata;
    }

    protected Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JiveContent content) {
        metadata.createdOn(Optional.ofNullable(content.published));
        metadata.lastUpdated(Optional.ofNullable(content.updated));

        if(content.resources != null && content.resources.containsKey("html")){
            try {
                metadata.remoteUri(Optional.of(new URI(content.resources.get("html").ref)));
            } catch (Exception e) {}
        }
        if(content.parentPlace != null){
            if(StringUtils.isNotBlank(content.parentPlace.html)){
                metadata.extraProps().get().put("jiveParentUri", content.parentPlace.html);
            }
            if(StringUtils.isNotBlank(content.parentPlace.placeID)){
                metadata.extraProps().get().put("jiveParentPlaceId", content.parentPlace.placeID);
            }
            if(StringUtils.isNotBlank(content.parentPlace.uri)){
                metadata.extraProps().get().put("jiveParentApiUri", content.parentPlace.uri);
            }
        }
        if(StringUtils.isNotBlank(content.contentID)){
            metadata.extraProps().get().put("jiveContentId", content.contentID);
        }

        log.debug(String.format("Updated %s metadata from response content.", metadata.sourcePath().toAbsolutePath().toString()));
        return metadata;
    }

    protected Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, PagedJivePlace places) {
        if(places != null && places.list != null && places.list.size() > 0){
            return updateMetadata(metadata, places.list.get(0));
        }

        return metadata;
    }

    protected Config.DocumentMetadata updateMetadata(Config.DocumentMetadata metadata, JivePlace place) {
        if(StringUtils.isNotBlank(place.placeID)){
            metadata.extraProps().get().put("jiveParentPlaceId", place.placeID);
        }
        if(place.resources != null && place.resources.containsKey("html")){
            metadata.extraProps().get().put("jiveParentUri", place.resources.get("html").ref);
        }
        if(place.resources != null && place.resources.containsKey("html")){
            metadata.extraProps().get().put("jiveParentApiUri", place.resources.get("self").ref);
        }

        log.debug(String.format("Updated %s metadata from response content.", metadata.sourcePath().toAbsolutePath().toString()));
        return metadata;
    }

    protected String jiveObjectId(URI remoteDoc){
        Pattern p = Pattern.compile(".*DOC-(\\d+)-*.*");
        Matcher m = p.matcher(remoteDoc.getPath());
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to extract jive object id from %s.", remoteDoc.toString()));
        }
    }

    protected String jiveParentPlaceName(String parentPlaceUrl){
        Pattern p = Pattern.compile(".*/(.*)");
        Matcher m = p.matcher(parentPlaceUrl);
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to parent place name from %s.", parentPlaceUrl));
        }
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html
     *
     * @param metadata
     * @return
     */
    protected String documentPostBody(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveContent jiveDocument = new JiveContent();
        jiveDocument.parentPlace = null;
        jiveDocument.subject = metadata.title();
        jiveDocument.content.text = Resources.load(context.convertedPath(metadata).get().toString());
        jiveDocument.type = "document";
        jiveDocument.typeCode = 102;

        if(metadata.extraProps().get().containsKey("jiveContentId")){
            jiveDocument.contentID = metadata.extraProps().get().get("jiveContentId");
        }

        if(metadata.extraProps().get().containsKey("jiveParentApiUri")) {
            jiveDocument.parent = metadata.extraProps().get().get("jiveParentApiUri");
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
        public ParentPlace parentPlace = new ParentPlace();

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
