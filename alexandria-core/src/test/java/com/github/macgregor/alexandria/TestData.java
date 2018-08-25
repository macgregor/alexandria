package com.github.macgregor.alexandria;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;

public class TestData {
    public static final String EXCLUDE = "*.ignore";
    public static final String INCLUDE = "*.md";

    public static Context completeContext(TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Path base = temporaryFolder.getRoot().toPath().toAbsolutePath();
        Context context = new Context();
        context.projectBase(base);
        context.outputPath(Optional.of(temporaryFolder.getRoot().toPath()));
        context.exclude(Collections.singletonList(EXCLUDE));
        context.include(Collections.singletonList(INCLUDE));
        context.searchPath(Collections.singletonList(base));
        context.config(completeConfig(temporaryFolder));
        return context;
    }

    public static Context minimalContext(TemporaryFolder temporaryFolder) throws IOException {
        Path base = temporaryFolder.getRoot().toPath().toAbsolutePath();
        Context context = new Context();
        context.projectBase(base);
        context.searchPath(Collections.singletonList(base));
        context.include(Collections.singletonList(INCLUDE));

        context.outputPath(Optional.empty());
        context.exclude(Collections.emptyList());
        context.config(minimalConfig(temporaryFolder));
        return context;
    }

    public static Config completeConfig(TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Config config = new Config();
        config.defaultTags(Optional.of(Collections.singletonList("default")));
        config.remote(completeRemoteConfig());
        config.metadata(Optional.of(Arrays.asList(
                documentForCreate(temporaryFolder),
                documentForDelete(temporaryFolder),
                documentForUpdate(temporaryFolder),
                minimalDocumentMetadata(temporaryFolder, "doc.ignore"))));
        return config;
    }

    public static Config minimalConfig(TemporaryFolder temporaryFolder) throws IOException {
        Config config = new Config();
        config.defaultTags(Optional.empty());
        config.remote(minimalRemoteConfig());
        config.metadata(Optional.ofNullable(Collections.singletonList(minimalDocumentMetadata(temporaryFolder, "minimal.md"))));
        return config;
    }

    public static Config.DocumentMetadata documentForCreate(TemporaryFolder temporaryFolder) throws IOException {
        return minimalDocumentMetadata(temporaryFolder, "create.md");
    }

    public static Config.DocumentMetadata documentForUpdate(TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Config.DocumentMetadata metadata = completeDocumentMetadata(temporaryFolder, "update.md");
        metadata.deletedOn(Optional.empty());
        metadata.sourceChecksum(Optional.of(-1L));
        metadata.extraProps().get().remove("delete");
        return metadata;
    }

    public static Config.DocumentMetadata documentForDelete(TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        Config.DocumentMetadata metadata = minimalDocumentMetadata(temporaryFolder, "delete.md");
        FileUtils.deleteQuietly(metadata.sourcePath().toFile());
        metadata.extraProps(Optional.of(Collections.singletonMap("delete", "true")));
        return metadata;
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(TemporaryFolder temporaryFolder, String name) throws IOException {
        Path path = temporaryFolder.getRoot().toPath().relativize(temporaryFolder.newFile(name).toPath());
        Resources.save(path.toString(), String.format("# %s\n\nHello", name));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(path);
        metadata.title(name);

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

    public static Config.DocumentMetadata completeDocumentMetadata(TemporaryFolder temporaryFolder, String name) throws IOException, URISyntaxException {
        Path path = temporaryFolder.getRoot().toPath().relativize(temporaryFolder.newFile(name).toPath());
        Path converted = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), FilenameUtils.getBaseName(path.toFile().getName()) + ".html");
        Resources.save(path.toString(), String.format("# %s\n\nHello", name));
        Markdown.toHtml(path, converted);
        long sourceCheckSum = FileUtils.checksumCRC32(path.toFile());
        long convertedCheckSum = FileUtils.checksumCRC32(converted.toFile());

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(path);
        metadata.title(name);
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
}
