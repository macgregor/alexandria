package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Markdown;
import com.github.macgregor.alexandria.Resources;
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

    public static Context minimalJiveContext(TemporaryFolder temporaryFolder) throws IOException {
        Context context = minimalContext(temporaryFolder);
        minimalJiveConfig(context, temporaryFolder);
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

    public static Config minimalJiveConfig(Context context, TemporaryFolder temporaryFolder) throws IOException {
        Config config = minimalConfig(context, temporaryFolder);
        config.remote(minimalJiveRemoteConfig());
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
        Markdown.toHtml(context, path, converted);
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

    public static Config.RemoteConfig minimalJiveRemoteConfig(){
        Config.RemoteConfig remoteConfig = minimalRemoteConfig();
        remoteConfig.clazz("com.github.macgregor.alexandria.remotes.JiveRemote");
        remoteConfig.password(Optional.of(""));
        remoteConfig.username(Optional.of(""));
        remoteConfig.baseUrl(Optional.of(""));
        return remoteConfig;
    }
}