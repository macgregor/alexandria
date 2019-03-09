package com.github.macgregor.alexandria.flexmark;

import com.github.macgregor.alexandria.AlexandriaConvert;
import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.markdown.NoopMarkdownConverter;
import com.github.macgregor.alexandria.remotes.NoopRemote;
import com.github.macgregor.alexandria.remotes.Remote;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

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

        Remote remote = new NoopRemote();
        remote.markdownConverter(new NoopMarkdownConverter());
        context.remote(Optional.of(remote));

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

        Remote remote = new NoopRemote();
        remote.markdownConverter(new NoopMarkdownConverter());
        context.remote(Optional.of(remote));

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

    public static Config.DocumentMetadata documentForCreate(Context context, Path documentPath) throws IOException, URISyntaxException {
        Config.DocumentMetadata metadata = completeDocumentMetadata(context, documentPath);
        metadata.remoteUri(Optional.empty());
        metadata.deletedOn(Optional.empty());
        metadata.sourceChecksum(Optional.empty());
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
        return minimalDocumentMetadata(path);
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(Context context, TemporaryFolder temporaryFolder) throws IOException {
        Config.DocumentMetadata metadata = minimalDocumentMetadata(temporaryFolder);
        context.addMetadata(metadata);
        return metadata;
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(Context context, Path documentPath) throws IOException {
        Config.DocumentMetadata metadata = minimalDocumentMetadata(documentPath);
        context.addMetadata(metadata);
        return metadata;
    }

    public static Config.DocumentMetadata minimalDocumentMetadata(Path documentPath) throws IOException {
        Path path = documentPath.toAbsolutePath();
        if(!path.toFile().exists()){
            path.toFile().createNewFile();
        }
        String fileName = path.toFile().getName();
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

    public static Config.DocumentMetadata completeDocumentMetadata(Context context, Path documentPath) throws IOException, URISyntaxException {
        Path path = documentPath.toAbsolutePath();
        if(!path.toFile().exists()){
            path.toFile().createNewFile();
        }
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();

        String fileName = path.toFile().getName();
        long sourceCheckSum = FileUtils.checksumCRC32(path.toFile());
        metadata.sourcePath(path);
        metadata.sourceChecksum(Optional.of(sourceCheckSum));

        String intermediateConvertedFileName = String.format("%s-%s-int.md", FilenameUtils.getBaseName(fileName), path.getParent().toString().hashCode());
        Path intermediate = Paths.get(context.outputPath().orElse(path.getParent()).toString(), intermediateConvertedFileName);
        Resources.save(intermediate.toString(), String.format("# %s\n\nIntermediate%s%s", fileName,
                AlexandriaConvert.FOOTER_SEPARATOR,
                AlexandriaConvert.DEFAULT_FOOTER));
        metadata.intermediateConvertedPath(Optional.of(intermediate));

        String convertedFileName = String.format("%s-%s-fin.md", FilenameUtils.getBaseName(fileName), path.getParent().toString().hashCode());
        Path converted = Paths.get(context.outputPath().orElse(path.getParent()).toString(), convertedFileName);
        Resources.save(converted.toString(), String.format("# %s\n\nConverted%s%s", fileName,
                AlexandriaConvert.FOOTER_SEPARATOR,
                AlexandriaConvert.DEFAULT_FOOTER));
        long convertedCheckSum = FileUtils.checksumCRC32(converted.toFile());
        metadata.convertedPath(Optional.of(converted));
        metadata.convertedChecksum(Optional.of(convertedCheckSum));
        context.convertedPath(metadata, converted);

        metadata.title(fileName);
        metadata.remoteUri(Optional.of(new URI("https://jive.com/api/core/v3/contents/DOC-1234")));
        metadata.tags(Optional.of(Collections.singletonList("complete")));
        metadata.createdOn(Optional.of(ZonedDateTime.now()));
        metadata.lastUpdated(Optional.of(ZonedDateTime.now()));
        metadata.deletedOn(Optional.of(ZonedDateTime.now()));

        context.addMetadata(metadata);
        return metadata;
    }

    public static Config.DocumentMetadata completeDocumentMetadata(Context context, TemporaryFolder temporaryFolder) throws IOException, URISyntaxException {
        String fileName = UUID.randomUUID().toString() + ".md";
        Path path = temporaryFolder.newFile(fileName).toPath().toAbsolutePath();
        Resources.save(path.toString(),  String.format("# %s\n\nSource file", fileName));
        return completeDocumentMetadata(context, path);
    }

    public static Config.RemoteConfig completeRemoteConfig(){
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        remoteConfig.converterClazz("com.github.macgregor.alexandria.markdown.NoopMarkdownConverter");
        remoteConfig.datetimeFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        remoteConfig.requestTimeout(1);

        remoteConfig.password(Optional.of("password"));
        remoteConfig.username(Optional.of("username"));
        remoteConfig.baseUrl(Optional.of("https://jive.com/api/core/v3"));

        return remoteConfig;
    }

    public static Config.RemoteConfig minimalRemoteConfig(){
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.clazz("com.github.macgregor.alexandria.remotes.NoopRemote");
        remoteConfig.converterClazz("com.github.macgregor.alexandria.markdown.NoopMarkdownConverter");
        remoteConfig.baseUrl(Optional.of("https://www.jive.com"));
        remoteConfig.datetimeFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        remoteConfig.requestTimeout(1);

        remoteConfig.password(Optional.empty());
        remoteConfig.username(Optional.empty());

        return remoteConfig;
    }

    public static Path newFile(File parent, String path) throws IOException {
        if(path.startsWith("/")){
            path = path.replaceFirst("/", path);
        }
        String[] segments = path.split("/");
        Path p = Paths.get(parent.getAbsolutePath(), segments);
        return newFile(p);
    }

    public static Path newFile(Path path) throws IOException {
        path.getParent().toFile().mkdirs();
        path.toFile().createNewFile();
        return path;
    }
}
