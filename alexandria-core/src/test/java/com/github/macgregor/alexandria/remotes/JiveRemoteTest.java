package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.DocumentMetadata;
import com.github.macgregor.alexandria.Resources;
import com.google.gson.Gson;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JiveRemoteTest {

    @Test
    public void voidTestJivePagedContentParsing() throws IOException {
        Gson gson = new Gson();
        JiveRemote.PagedJiveContent parsed = gson.fromJson(Resources.load("src/test/resources/DOC-1072237.json"), JiveRemote.PagedJiveContent.class);

        assertThat(parsed).isEqualToComparingFieldByFieldRecursively(expectedPagedJiveContent());
    }

    @Test
    public void testDocumentExistsCall() throws IOException, URISyntaxException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")));
        server.start();

        HttpUrl baseUrl = server.url("api/core/v3");
        RestRemoteConfig config = new RestRemoteConfig();
        config.setBaseUrl(baseUrl.toString());
        JiveRemote jiveRemote = new JiveRemote(config);

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setRemote(Optional.of(new URI("https://mojo.redhat.com/docs/DOC-1072237")));
        assertThat(jiveRemote.exists(metadata)).isTrue();
    }

    @Test
    public void testDocumentExistsFalseWhenNoRemoteSet() throws IOException {
        RestRemoteConfig config = new RestRemoteConfig();
        JiveRemote jiveRemote = new JiveRemote(config);
        DocumentMetadata metadata = new DocumentMetadata();
        assertThat(jiveRemote.exists(metadata)).isFalse();
    }

    @Test
    public void testDocumentExistsFalseOn404() throws IOException, URISyntaxException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(404));
        server.start();

        HttpUrl baseUrl = server.url("api/core/v3");
        RestRemoteConfig config = new RestRemoteConfig();
        config.setBaseUrl(baseUrl.toString());
        JiveRemote jiveRemote = new JiveRemote(config);

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setRemote(Optional.of(new URI("https://mojo.redhat.com/docs/DOC-1072237")));
        assertThat(jiveRemote.exists(metadata)).isFalse();
    }

    @Test
    public void testDocumentExistsUpdatesMetadata() throws IOException, URISyntaxException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")));
        server.start();

        HttpUrl baseUrl = server.url("api/core/v3");
        RestRemoteConfig config = new RestRemoteConfig();
        config.setBaseUrl(baseUrl.toString());
        JiveRemote jiveRemote = new JiveRemote(config);

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setRemote(Optional.of(new URI("https://mojo.redhat.com/docs/DOC-1072237")));
        assertThat(jiveRemote.exists(metadata)).isTrue();
        assertThat(metadata.getLastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.getCreatedOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.getExtra().get("jiveParentUrl")).isEqualTo("https://mojo.redhat.com/groups/soa-services-esb");
        assertThat(metadata.getExtra().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.getExtra().get("jiveContentId")).isEqualTo("1278973");
    }

    private JiveRemote.PagedJiveContent expectedPagedJiveContent(){
        JiveRemote.JiveContent jiveContent = new JiveRemote.JiveContent();
        jiveContent.id = 1072237;
        jiveContent.published = "2016-03-21T15:07:34.533+0000";
        jiveContent.updated = "2018-06-22T18:42:59.652+0000";
        jiveContent.tags = new String[]{"esb", "unified-messagebus", "umb"};
        jiveContent.contentID = "1278973";
        jiveContent.parent = "https://mojo.redhat.com/api/core/v3/places/61562";
        jiveContent.subject = "Unified Message Bus (UMB) Documentation Index";
        jiveContent.type = "document";
        jiveContent.typeCode = 102;

        JiveRemote.JiveContent.Content content = new JiveRemote.JiveContent.Content();
        content.editable = false;
        content.type = "text/html";
        content.text = "<body></body>";
        jiveContent.content = content;

        JiveRemote.JiveContent.ParentPlace parentPlace = new JiveRemote.JiveContent.ParentPlace();
        parentPlace.id = 2276;
        parentPlace.html = "https://mojo.redhat.com/groups/soa-services-esb";
        parentPlace.placeID = "61562";
        parentPlace.name = "SOA Services and ESB";
        parentPlace.type = "group";
        parentPlace.uri = "https://mojo.redhat.com/api/core/v3/places/61562";
        jiveContent.parentPlace = parentPlace;

        Map<String, JiveRemote.JiveContent.Link> resources = new HashMap<>();
        resources.put("html", link("https://mojo.redhat.com/docs/DOC-1072237", "GET"));
        resources.put("extprops", link("https://mojo.redhat.com/api/core/v3/contents/1278973/extprops", "POST", "DELETE", "GET"));
        jiveContent.resources = resources;

        JiveRemote.PagedJiveContent pagedContent = new JiveRemote.PagedJiveContent();
        pagedContent.itemsPerPage = 1;
        pagedContent.links.put("next", "https://mojo.redhat.com/api/core/v3/contents?sort=dateCreatedDesc&fields=id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent&filter=entityDescriptor%28102,1072237%29&abridged=false&includeBlogs=false&count=1&startIndex=1");
        pagedContent.startIndex = 0;
        pagedContent.list = new JiveRemote.JiveContent[]{jiveContent};
        return pagedContent;
    }

    private JiveRemote.JiveContent.Link link(String ref, String... allowed){
        JiveRemote.JiveContent.Link link = new JiveRemote.JiveContent.Link();
        link.ref = ref;
        link.allowed = allowed;
        return link;
    }
}
