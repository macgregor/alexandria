package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.remotes.jive.JiveData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TestData {
    public static final String EXCLUDE = "*.ignore";
    public static final String INCLUDE = "*.md";

    public static Context completeContext(TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Path base = temporaryFolder.getRoot().toPath().toAbsolutePath();
        Context context = new Context();
        context.configPath(Paths.get(base.toString(), ".alexandria"));
        context.projectBase(base);
        context.outputPath(Optional.of(temporaryFolder.getRoot().toPath()));
        context.exclude(Collections.singletonList(EXCLUDE));
        context.include(Collections.singletonList(INCLUDE));
        context.searchPath(Collections.singletonList(base));
        context.makePathsAbsolute();
        completeConfig(context, temporaryFolder);
        return context;
    }

    public static Context minimalContext(TemporaryFolder temporaryFolder) throws IOException {
        Path base = temporaryFolder.getRoot().toPath().toAbsolutePath();
        Context context = new Context();
        context.configPath(Paths.get(base.toString(), ".alexandria"));
        context.projectBase(base);
        context.searchPath(Collections.singletonList(base));
        context.include(Collections.singletonList(INCLUDE));
        context.outputPath(Optional.empty());
        context.exclude(Collections.emptyList());
        minimalConfig(context, temporaryFolder);
        context.makePathsAbsolute();
        return context;
    }

    public static Config completeConfig(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Config config = new Config();
        context.config(config);
        config.defaultTags(Optional.of(Collections.singletonList("default")));
        config.remote(completeRemoteConfig());
        config.metadata(Optional.of(new ArrayList<>()));
        documentForCreate(context, temporaryFolder);
        documentForDelete(context, temporaryFolder);
        documentForUpdate(context, temporaryFolder);
        minimalDocumentMetadata(context, temporaryFolder);

        return config;
    }

    public static Config minimalConfig(Context context, TemporaryFolder temporaryFolder) throws IOException {
        Config config = new Config();
        context.config(config);
        config.defaultTags(Optional.empty());
        config.remote(minimalRemoteConfig());
        config.metadata(Optional.of(new ArrayList<>()));
        minimalDocumentMetadata(context, temporaryFolder);
        return config;
    }

    public static Config.DocumentMetadata documentForCreate(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        context.configPath(Paths.get(temporaryFolder.getRoot().toPath().toAbsolutePath().toString(), ".alexandria"));
        Config.DocumentMetadata metadata = completeDocumentMetadata(context, temporaryFolder);
        metadata.remoteUri(Optional.empty());
        metadata.deletedOn(Optional.empty());
        metadata.sourceChecksum(Optional.empty());
        metadata.extraProps().get().remove("delete");
        return metadata;
    }

    public static Config.DocumentMetadata documentForUpdate(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        context.configPath(Paths.get(temporaryFolder.getRoot().toPath().toAbsolutePath().toString(), ".alexandria"));
        Config.DocumentMetadata metadata = completeDocumentMetadata(context, temporaryFolder);
        metadata.deletedOn(Optional.empty());
        metadata.sourceChecksum(Optional.of(-1L));
        metadata.extraProps().get().remove("delete");
        return metadata;
    }

    public static Config.DocumentMetadata documentForDelete(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        context.configPath(Paths.get(temporaryFolder.getRoot().toPath().toAbsolutePath().toString(), ".alexandria"));
        Config.DocumentMetadata metadata = minimalDocumentMetadata(context, temporaryFolder);
        FileUtils.forceDelete(metadata.sourcePath().toFile());
        metadata.remoteUri(Optional.of(new URI("https://jive.com/api/core/v3/contents/DOC-1234")));
        return metadata;
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(TemporaryFolder temporaryFolder) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".md";
        Path path = temporaryFolder.newFile(fileName).toPath().toAbsolutePath();

        Resources.save(path.toString(), String.format("# %s\n\nHello", fileName));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(path);
        metadata.title(fileName);

        metadata.extraProps(Optional.empty());
        metadata.remoteUri(Optional.empty());
        metadata.sourceChecksum(Optional.empty());
        metadata.tags(Optional.empty());
        metadata.deletedOn(Optional.empty());
        metadata.createdOn(Optional.empty());
        metadata.lastUpdated(Optional.empty());
        metadata.convertedChecksum(Optional.empty());

        return metadata;
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(Context context, TemporaryFolder temporaryFolder) throws IOException {
        Config.DocumentMetadata metadata = minimalDocumentMetadata(temporaryFolder);
        context.addMetadata(metadata);
        return metadata;
    }

    public static Config.DocumentMetadata completeDocumentMetadata(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        String fileName = UUID.randomUUID().toString() + ".md";
        Path path = temporaryFolder.newFile(fileName).toPath().toAbsolutePath();
        Path converted = Paths.get(path.getParent().toString(), FilenameUtils.getBaseName(path.toFile().getName()) + ".html");
        Resources.save(path.toString(), String.format("# %s\n\nHello", fileName));
        Markdown.toHtml(path, converted);
        long sourceCheckSum = FileUtils.checksumCRC32(path.toFile());
        long convertedCheckSum = FileUtils.checksumCRC32(converted.toFile());

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(path);
        metadata.title(fileName);
        metadata.remoteUri(Optional.of(new URI("https://jive.com/api/core/v3/contents/DOC-1234")));
        metadata.sourceChecksum(Optional.of(sourceCheckSum));
        metadata.tags(Optional.of(Collections.singletonList("complete")));
        metadata.createdOn(Optional.of(ZonedDateTime.now()));
        metadata.lastUpdated(Optional.of(ZonedDateTime.now()));
        metadata.deletedOn(Optional.of(ZonedDateTime.now()));
        metadata.convertedChecksum(Optional.of(convertedCheckSum));

        Map<String, String> extraProps = new HashMap<>();
        extraProps.put("convertedPath", converted.toString());
        extraProps.put("delete", "true");
        metadata.extraProps(Optional.of(extraProps));

        context.convertedPath(metadata, converted);

        context.addMetadata(metadata);

        return metadata;
    }

    public static Config.RemoteConfig completeRemoteConfig(){
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        remoteConfig.datetimeFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        remoteConfig.requestTimeout(1);
        remoteConfig.supportsNativeMarkdown(false);

        remoteConfig.password(Optional.of("password"));
        remoteConfig.username(Optional.of("username"));
        remoteConfig.baseUrl(Optional.of("https://jive.com/api/core/v3"));

        return remoteConfig;
    }

    public static Config.RemoteConfig minimalRemoteConfig(){
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        remoteConfig.datetimeFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        remoteConfig.requestTimeout(1);
        remoteConfig.supportsNativeMarkdown(false);

        remoteConfig.password(Optional.empty());
        remoteConfig.username(Optional.empty());
        remoteConfig.baseUrl(Optional.empty());

        return remoteConfig;
    }

    public static JiveData.PagedJiveContent expectedPagedJiveContent(){
        JiveData.JiveContent jiveContent = new JiveData.JiveContent();
        jiveContent.id = 1072237;
        jiveContent.published = ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        jiveContent.updated = ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        jiveContent.tags = Arrays.asList("foo", "bar", "baz");
        jiveContent.contentID = "1278973";
        jiveContent.parent = "https://jive.com/api/core/v3/places/61562";
        jiveContent.subject = "Document Title";
        jiveContent.type = "document";
        jiveContent.typeCode = 102;

        JiveData.JiveContent.Content content = new JiveData.JiveContent.Content();
        content.editable = true;
        content.type = "text/html";
        content.text = "<body></body>";
        jiveContent.content = content;

        JiveData.JiveContent.ParentPlace parentPlace = new JiveData.JiveContent.ParentPlace();
        parentPlace.id = 2276;
        parentPlace.html = "https://jive.com/groups/parent_group";
        parentPlace.placeID = "61562";
        parentPlace.name = "Some Parent Group";
        parentPlace.type = "group";
        parentPlace.uri = "https://jive.com/api/core/v3/places/61562";
        jiveContent.parentPlace = parentPlace;

        Map<String, JiveData.Link> resources = new HashMap<>();
        resources.put("html", link("https://jive.com/docs/DOC-1072237", Arrays.asList("GET")));
        resources.put("extprops", link("https://jive.com/api/core/v3/contents/1278973/extprops", Arrays.asList("POST", "DELETE", "GET")));
        jiveContent.resources = resources;

        JiveData.PagedJiveContent pagedContent = new JiveData.PagedJiveContent();
        pagedContent.itemsPerPage = 1;
        pagedContent.links.put("next", "https://jive.com/api/core/v3/contents?sort=dateCreatedDesc&fields=id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent&filter=entityDescriptor%28102,1072237%29&abridged=false&includeBlogs=false&count=1&startIndex=1");
        pagedContent.startIndex = 0;
        pagedContent.list = Arrays.asList(jiveContent);
        return pagedContent;
    }

    public static JiveData.Link link(String ref, List<String> allowed){
        JiveData.Link link = new JiveData.Link();
        link.ref = ref;
        link.allowed = allowed;
        return link;
    }
}
