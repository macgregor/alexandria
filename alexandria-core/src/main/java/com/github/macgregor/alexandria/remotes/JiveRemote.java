package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.DocumentMetadata;
import com.github.macgregor.alexandria.Resources;
import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JiveRemote implements Remote{

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String STANDARD_FIELD_PROJECTION = "id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent";
    public static final DateTimeFormatter JIVE_DATETIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ");

    protected OkHttpClient client;
    protected RestRemoteConfig config;

    public JiveRemote(OkHttpClient client, RestRemoteConfig config){
        this.client = client;
        this.config = config;
    }

    public JiveRemote(RestRemoteConfig config){
        this(new OkHttpClient(), config);
    }

    @Override
    public boolean exists(DocumentMetadata documentMetadata) throws IOException {
        if(!documentMetadata.getRemote().isPresent()){
            return false;
        }

        // https://community.jivesoftware.com/docs/DOC-153931
        String filter = String.format("entityDescriptor(102,%s)", jiveObjectId(documentMetadata.getRemote().get()));

        HttpUrl route = HttpUrl.parse(config.getBaseUrl()).newBuilder()
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

        Call call = client.newCall(request);
        Response response = call.execute();

        if(response.code() == 200){
            Gson gson = new Gson();
            PagedJiveContent pagedJiveContent = gson.fromJson(response.body().string(), PagedJiveContent.class);
            updateMetadata(documentMetadata, pagedJiveContent);
            return true;
        } else if(response.code() == 404){
            return false;
        } else{
            throw new IOException(String.format("Unexpected status code %d checking if %s exists.", response.code(), documentMetadata.getRemote().get()));
        }
    }

    @Override
    public URI create(DocumentMetadata documentMetadata) throws IOException {

        HttpUrl route = HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegment("contents")
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .post(RequestBody.create(JSON, documentPostBody(documentMetadata)))
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if(!response.isSuccessful()){
            throw new IOException(String.format("Unexpected status code %d creating %s.", response.code(), documentMetadata.getConverted().get()));
        }

        Gson gson = new Gson();
        JiveContent jiveContent = gson.fromJson(response.body().string(), JiveContent.class);
        updateMetadata(documentMetadata, jiveContent);

        return documentMetadata.getRemote().get();
    }

    @Override
    public URI update(DocumentMetadata documentMetadata) throws IOException {
        HttpUrl route = HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegment("contents")
                .addPathSegment(jiveObjectId(documentMetadata.getRemote().get()))
                .build();

        Request request = authenticated(new Request.Builder())
                .url(route)
                .post(RequestBody.create(JSON, documentPostBody(documentMetadata)))
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if(!response.isSuccessful()){
            throw new IOException(String.format("Unexpected status code %d updating %s.", response.code(), documentMetadata.getRemote().get()));
        }

        Gson gson = new Gson();
        JiveContent jiveContent = gson.fromJson(response.body().string(), JiveContent.class);
        updateMetadata(documentMetadata, jiveContent);

        return documentMetadata.getRemote().get();
    }

    @Override
    public Request.Builder authenticated(Request.Builder builder) {
        if(config.getOauthToken().isPresent()){
            builder.addHeader("Authorization: Bearer", config.getOauthToken().get());
        } else if(config.getUsername().isPresent() && config.getPassword().isPresent()){
            builder.addHeader("Authorization", Credentials.basic(config.getUsername().get(), config.getPassword().get()));
        }
        return builder;
    }

    protected DocumentMetadata updateMetadata(DocumentMetadata metadata, PagedJiveContent content) {
        if(content != null && content.list != null && content.list.length > 0){
            return updateMetadata(metadata, content.list[0]);
        }

        return metadata;
    }

    protected DocumentMetadata updateMetadata(DocumentMetadata metadata, JiveContent content) {
        if(StringUtils.isNotBlank(content.published)){
            metadata.setCreatedOn(Optional.of(ZonedDateTime.parse(content.published, JIVE_DATETIME)));
        }
        if(StringUtils.isNotBlank(content.updated)){
            metadata.setLastUpdated(Optional.of(ZonedDateTime.parse(content.updated, JIVE_DATETIME)));
        }
        if(content.resources != null && content.resources.containsKey("html")){
            try {
                metadata.setRemote(Optional.of(new URI(content.resources.get("html").ref)));
            } catch (URISyntaxException e) {}
        }
        if(content.parentPlace != null){
            if(StringUtils.isNotBlank(content.parentPlace.html)){
                metadata.getExtra().put("jiveParentUrl", content.parentPlace.html);
            }
            if(StringUtils.isNotBlank(content.parentPlace.placeID)){
                metadata.getExtra().put("jiveParentPlaceId", content.parentPlace.placeID);
            }
        }
        if(StringUtils.isNotBlank(content.contentID)){
            metadata.getExtra().put("jiveContentId", content.contentID);
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
    protected String documentPostBody(DocumentMetadata metadata) throws IOException {
        Gson gson = new Gson();
        JiveContent jiveDocument = new JiveContent();

        if(metadata.getExtra().containsKey("jiveContentId")){
            jiveDocument.contentID = metadata.getExtra().get("jiveContentId");
        }
        jiveDocument.subject = metadata.getTitle();

        if(metadata.getTags().isPresent()){
            jiveDocument.tags = metadata.getTags().get().toArray(new String[]{});
        }

        jiveDocument.content.text = Resources.load(metadata.getConverted().get());

        //TODO: add parent

        return gson.toJson(jiveDocument);
    }

    public static class PagedJiveContent{
        public Map<String, String> links = new HashMap<>();
        public JiveContent[] list;
        public Integer startIndex;
        public Integer itemsPerPage;
    }

    public static class JiveContent{
        public Integer id;
        public String contentID;
        public String published;
        public String updated;
        public String[] tags;
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
            public String[] allowed;
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
