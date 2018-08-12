package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.AlexandriaConfig;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.Resources;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class JiveRemote implements Remote{

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String STANDARD_FIELD_PROJECTION = "id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent";

    protected OkHttpClient client;
    protected AlexandriaConfig.RemoteConfig config;

    public JiveRemote(OkHttpClient client, AlexandriaConfig.RemoteConfig config){
        this.client = client;
        this.config = config;
    }

    public JiveRemote(AlexandriaConfig.RemoteConfig config){
        this(new OkHttpClient(), config);
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#getContents(List%3CString%3E,%20String,%20int,%20int,%20String,%20boolean,%20boolean)
     *
     * @param metadata
     * @throws IOException
     */
    @Override
    public void syncMetadata(AlexandriaConfig.DocumentMetadata metadata) throws IOException {
        if(metadata.remoteUri().isPresent()) {
            // https://community.jivesoftware.com/docs/DOC-153931
            String filter = String.format("entityDescriptor(102,%s)", jiveObjectId(metadata.remoteUri().get()));

            HttpUrl route = HttpUrl.parse(config.baseUrl()).newBuilder()
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
                HttpException exception = new HttpException("Cannot parse response content", e);
                exception.setRequest(Optional.of(request));
                exception.setResponse(Optional.ofNullable(response));
                throw exception;
            }
        }
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#createContent(String,%20String,%20String,%20String)
     *
     * @param metadata
     * @throws IOException
     */
    @Override
    public void create(AlexandriaConfig.DocumentMetadata metadata) throws IOException {

        HttpUrl route = HttpUrl.parse(config.baseUrl()).newBuilder()
                .addPathSegment("contents")
                .addQueryParameter("fields", STANDARD_FIELD_PROJECTION)
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .post(RequestBody.create(JSON, documentPostBody(metadata)))
                .build();

        Response response = doRequest(request);
        try {
            JiveContent jiveContent =  Jackson.jsonMapper().readValue(response.body().charStream(), JiveContent.class);
            updateMetadata(metadata, jiveContent);
        } catch (IOException e) {
            HttpException exception = new HttpException("Cannot parse response content", e);
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
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
    public void update(AlexandriaConfig.DocumentMetadata metadata) throws IOException {
        HttpUrl route = HttpUrl.parse(config.baseUrl()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(jiveObjectId(metadata.remoteUri().get()))
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .put(RequestBody.create(JSON, documentPostBody(metadata)))
                .build();

        Response response = doRequest(request);
        try {
            JiveContent jiveContent =  Jackson.jsonMapper().readValue(response.body().charStream(), JiveContent.class);
            updateMetadata(metadata, jiveContent);
        } catch (IOException e) {
            HttpException exception = new HttpException("Cannot parse response content", e);
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        }
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/ContentService.html#deleteContent(String,%20Boolean)
     *
     * @param metadata
     * @throws IOException
     */
    @Override
    public void delete(AlexandriaConfig.DocumentMetadata metadata) throws IOException {
        HttpUrl route = HttpUrl.parse(config.baseUrl()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(jiveObjectId(metadata.remoteUri().get()))
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .delete()
                .build();

        Response response = doRequest(request);
        metadata.deletedOn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    protected Response doRequest(Request request) throws HttpException {
        Call call = client.newCall(request);

        Response response = null;
        try {
            response = call.execute();
        } catch(IOException e){
            HttpException exception = new HttpException(String.format("Unable to make request %s", request.url().toString()), e);
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        }

        if(response.isSuccessful()){
            return response;
        } else if(response.code() == 400){
            HttpException exception = new HttpException("Bad request.");
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        } else if(response.code() == 403){
            HttpException exception = new HttpException("Unauthorized to access document.");
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        } else if(response.code() == 404){
            HttpException exception = new HttpException("Document doesnt exist.");
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        } else if(response.code() == 409){
            HttpException exception = new HttpException("Document conflicts with existing document.");
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        } else {
            HttpException exception = new HttpException(String.format("Unexpected status code %d.", response.code()));
            exception.setRequest(Optional.of(request));
            exception.setResponse(Optional.ofNullable(response));
            throw exception;
        }
    }

    public Request.Builder authenticated(Request.Builder builder) {
        builder.addHeader("Authorization", Credentials.basic(config.username(), config.password()));
        return builder;
    }

    protected AlexandriaConfig.DocumentMetadata updateMetadata(AlexandriaConfig.DocumentMetadata metadata, PagedJiveContent content) {
        if(content != null && content.list != null && content.list.size() > 0){
            return updateMetadata(metadata, content.list.get(0));
        }

        return metadata;
    }

    protected AlexandriaConfig.DocumentMetadata updateMetadata(AlexandriaConfig.DocumentMetadata metadata, JiveContent content) {
        metadata.createdOn(Optional.ofNullable(content.published));
        metadata.lastUpdated(Optional.ofNullable(content.updated));

        if(content.resources != null && content.resources.containsKey("html")){
            try {
                metadata.remoteUri(Optional.of(new URI(content.resources.get("html").ref)));
            } catch (URISyntaxException e) {}
        }
        if(content.parentPlace != null){
            if(StringUtils.isNotBlank(content.parentPlace.html)){
                metadata.extraProps().get().put("jiveParentUrl", content.parentPlace.html);
            }
            if(StringUtils.isNotBlank(content.parentPlace.placeID)){
                metadata.extraProps().get().put("jiveParentPlaceId", content.parentPlace.placeID);
            }
        }
        if(StringUtils.isNotBlank(content.contentID)){
            metadata.extraProps().get().put("jiveContentId", content.contentID);
        }

        return metadata;
    }

    protected String jiveObjectId(URI remoteDoc){
        String[] segments = remoteDoc.getPath().split("/");
        return segments[segments.length-1].replace("DOC-", "");
    }

    /**
     * https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html
     *
     * @param metadata
     * @return
     */
    protected String documentPostBody(AlexandriaConfig.DocumentMetadata metadata) throws IOException {
        JiveContent jiveDocument = new JiveContent();

        if(metadata.extraProps().get().containsKey("jiveContentId")){
            jiveDocument.contentID = metadata.extraProps().get().get("jiveContentId");
        }
        jiveDocument.subject = metadata.title();

        if(metadata.tags().isPresent()){
            jiveDocument.tags = metadata.tags().get();
        }

        jiveDocument.content.text = Resources.load(metadata.convertedPath().get());

        //TODO: add parent

        return Jackson.jsonMapper().writeValueAsString(jiveDocument);
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

        public static class Link{
            public String ref;
            public List<String> allowed;
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
