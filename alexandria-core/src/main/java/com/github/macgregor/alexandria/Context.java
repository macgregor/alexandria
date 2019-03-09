package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;
import com.github.macgregor.alexandria.remotes.Remote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Runtime context containing arguments passed from the user agent (e.g. search path, files to include or exclude, etc).
 *
 * This information is not persisted by Alexandria as it is considered specific to the runtime environment at execution
 * that is not necessarily suitable distribution to others. Absolute paths are a good example of things that would
 * break others if they were committed to git.
 *
 * Any paths in the context should be absolute, while paths in {@link Config} are relative.
 */
@Data
@Slf4j
@Accessors(fluent = true)
@AllArgsConstructor @NoArgsConstructor
public class Context {

    /** Absolute path to the Alexandria {@link Config} file to load/save to. */
    @NonNull protected Path configPath;

    /** Absolute path to the project base where Alexandria is being executed */
    @NonNull protected Path projectBase;

    /** Absolute paths to use when searching for documents to index  */
    @NonNull protected List<Path> searchPath;

    /** Absolute path to output converted documents to. If not set, documents are converted in place where they are found. Default: none. */
    protected Optional<Path> outputPath = Optional.empty();

    /** File include patterns to use when searching for documents to index. See {@link PathFinder}. Default: *.md */
    protected List<String> include = new ArrayList<>(Arrays.asList("*.md"));

    /** File exclude patterns to use when searching for documents to index. See {@link PathFinder}. Default: none */
    protected List<String> exclude = new ArrayList<>();

    /** Enables adding a footer to converted documents warning readers edits to files on the remote will be overridden. Default: true */
    protected boolean disclaimerFooterEnabled = true;

    /** Path to a custom footer (markdown) file. If not set, a standard template is used. */
    protected Optional<Path> disclaimerFooterPath = Optional.empty();

    /** Working Aleandria config containing document index and remote config. */
    @NonNull protected Config config = new Config();

    /** Alexandria config originally loaded from the file system, kept in case of failures saving back to the filesystem. */
    @NonNull protected Config originalConfig = new Config();

    /** Cache for tracking absolute converted file paths for indexed metadata. Default: empty map. */
    protected Map<Config.DocumentMetadata, Path> convertedPaths = new HashMap<>();

    /** Remote that has been configured and initialized, can be used to retrieve the remote for any class or method that has the context */
    protected Optional<Remote> remote = Optional.empty();

    /**
     * Sets the path to the Alexandria config file. <b>Must be an absolute path</b>.
     *
     * Resolving relative paths is system dependent. If we dont have a predictable absolute path to use as a reference
     * point we may get into some weird states. It is up to the caller instantiating the context to properly set this
     * absolute path so that Alexandria can accurately create absolute and relative paths as it works.
     *
     * @param configPath  An absolute path to the Alexandria config file. Used to resolve all other relative and absolute paths.
     * @return  Alexandria context
     */
    public Context configPath(Path configPath){
        if(!configPath.isAbsolute()){
            this.configPath = configPath.toAbsolutePath();
        } else {
            this.configPath = configPath;
        }
        return this;
    }

    /**
     * Resolve a relative path to an absolute one using the {@link #configPath} directory. Useful for resolving relative
     * {@link com.github.macgregor.alexandria.Config.DocumentMetadata#sourcePath}.
     *
     * @param relativePath  relative path to convert to absolute
     * @return  the absolute path
     */
    public Path resolveRelativePath(Path relativePath){
        return configPath.getParent().resolve(relativePath);
    }

    /**
     * Convenience method for determining the size of the documentation index.
     *
     * @return {@link Config#metadata} size if present, otherwise 0
     */
    public int documentCount(){
        return config.metadata().isPresent() ? config.metadata().get().size() : 0;
    }

    /**
     * Convenience method to retrieve the absolute converted file path in the cache for the given document.
     *
     * @param metadata  indexed document to look for
     * @return  The converted path if it exist in the cache, otherwise Optional.empty()
     */
    public Optional<Path> convertedPath(Config.DocumentMetadata metadata){
        return convertedPaths.containsKey(metadata) ? Optional.of(convertedPaths.get(metadata)) : Optional.empty();
    }

    /**
     * Convenience method to add a absolute converted file path to the cache.
     *
     * @param metadata  indexed metadata that has been converted.
     * @param path  absolute path to the converted html file for the document.
     */
    public void convertedPath(Config.DocumentMetadata metadata, Path path){
        convertedPaths.put(metadata, path);
    }

    /**
     * Safetly add new document metadata to the index, converting the source path to absolute if necessary.
     *
     * @param metadata  metadata to add
     * @return  Alexandria context.
     */
    public Context addMetadata(Config.DocumentMetadata metadata){
        if(!config.metadata().isPresent()){
            config.metadata(Optional.of(new ArrayList<>()));
        }
        metadata.sourcePath(Resources.absolutePath(configPath.getParent(), metadata.sourcePath()));
        config.metadata().get().add(metadata);
        return this;
    }

    /**
     * Return the matching {@link Config.DocumentMetadata} for a {@link Path} if it exists.
     *
     * @param path  path to the potential document, will be made absolute as needed
     * @return  the matching {@link com.github.macgregor.alexandria.Config.DocumentMetadata} if it exists or
     *      Optional.empty() if it doesnt
     */
    public Optional<Config.DocumentMetadata> isIndexed(Path path){
        Path absolutePath = Resources.absolutePath(configPath().getParent(), path);
        if(config.metadata().isPresent()){
            for(Config.DocumentMetadata metadata : config.metadata().get()){
                if(metadata.sourcePath().equals(absolutePath)){
                    return Optional.of(metadata);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Convenience method for making a path relative to the {@link Context#configPath} absolute.
     *
     * @param path  relative path to be made absolute. It is assumed to be relative to {@link Context#configPath}
     * @return  absolute representation of the provided {@link Path}
     */
    public Path absolutePath(Path path){
        return Resources.absolutePath(configPath().getParent(), path);
    }

    /**
     * Make all {@link Config} and {@link Context} paths absolute relative to {@link #configPath}.
     *
     * @return  Alexandria context.
     */
    public Context makePathsAbsolute(){
        if( configPath != null){
            projectBase = Resources.absolutePath(configPath.getParent(), projectBase);
            outputPath = Optional.ofNullable(Resources.absolutePath(configPath.getParent(), outputPath.orElse(null)));
            searchPath = (List<Path>) Resources.absolutePath(configPath.getParent(), searchPath);
            if(disclaimerFooterPath.isPresent()){
                disclaimerFooterPath = Optional.of(Resources.absolutePath(configPath.getParent(), disclaimerFooterPath.get()));
            }
            if(config.metadata().isPresent()){
                config.metadata().get()
                        .stream()
                        .forEach(m -> {
                            m.sourcePath(Resources.absolutePath(configPath.getParent(), m.sourcePath()));
                        });
            }
        }
        return this;
    }

    /**
     * Make all {@link Config} and {@link Context} paths relative to {@link #configPath}.
     *
     * @return  Alexandria context.
     */
    public Context makePathsRelative(){
        if( configPath != null){
            projectBase = Resources.relativeTo(configPath.getParent(), projectBase);
            outputPath = Optional.ofNullable(Resources.relativeTo(configPath.getParent(), outputPath.orElse(null)));
            searchPath = (List<Path>) Resources.relativeTo(configPath.getParent(), searchPath);
            if(disclaimerFooterPath.isPresent()){
                disclaimerFooterPath = Optional.of(Resources.relativeTo(configPath.getParent(), disclaimerFooterPath.get()));
            }
            if(config.metadata().isPresent()){
                config.metadata().get()
                        .stream()
                        .forEach(m -> {
                            m.sourcePath(Resources.relativeTo(configPath.getParent(), m.sourcePath()));
                        });
            }
        }
        return this;
    }

    /**
     * Get tags for the document resolving default tags and document tags.
     *
     * @param metadata  document metadata to get tags for
     * @return  list of tags to add to the request or empty list if none are set
     */
    public List<String> getTagsForDocument(Config.DocumentMetadata metadata){
        List<String> tags = new ArrayList();
        if(config.defaultTags().isPresent()){
            tags.addAll(config.defaultTags().get());
        }
        if(metadata.tags().isPresent()){
            tags.addAll(metadata.tags().get());
        }
        return tags;
    }

    /**
     * Get extra properties for a document, resolving default remote extra props and document extra props.
     *
     * @param metadata  document metadata to get extra properties for
     * @return  map containing extra properties to use for processing a request
     */
    public Map<String, String> getExtraPropertiesForDocument(Config.DocumentMetadata metadata){
        Map<String, String> extraProps = new HashMap<>();
        if(config.remote().defaultExtraProps().isPresent()){
            extraProps.putAll(config.remote().defaultExtraProps().get());
        }
        if(metadata.extraProps().isPresent()){
            extraProps.putAll(metadata.extraProps().get());
        }
        return extraProps;
    }

    /**
     * Instantiate a {@link Remote} implementation based on the {@link Config#remote}.
     *
     * The class instantiation logic is very simple, but should be adequate for this simple use case. Essentially we just
     * pick the right class using the fully qualified class name in {@link com.github.macgregor.alexandria.Config.RemoteConfig#clazz}.
     * Implementation specific configuration and validation is delegated to the implementing class by calling
     * {@link Remote#configure(Config.RemoteConfig)} and {@link Remote#validateRemoteConfig()}.
     *
     * Only instantiates the remote implementation once, future calls to this method will return the value of {@link #remote}.
     *
     * @see com.github.macgregor.alexandria.remotes.NoopRemote
     *
     * @return  configured remote ready for use
     * @throws AlexandriaException  Exception wrapping any exception thrown instantiation, configuring or validating the remote
     */
    protected Remote configureRemote() throws AlexandriaException {
        if(this.remote.isPresent()){
            return this.remote.get();
        }

        Remote remote = Reflection.create(config().remote().clazz());
        Reflection.maybeImplementsInterface(remote, ContextAware.class)
                .ifPresent(r -> r.alexandriaContext(this));

        MarkdownConverter converter = Reflection.create(config().remote().converterClazz());
        Reflection.maybeImplementsInterface(converter, ContextAware.class)
                .ifPresent(c -> c.alexandriaContext(this));

        remote.markdownConverter(converter);
        remote.configure(config().remote());
        remote.validateRemoteConfig();
        this.remote = Optional.of(remote);
        return remote;
    }

    /**
     * Initialize Alexandria's {@link Context}, loading the {@link Config} from the given file path.
     *
     * Required context paths will be set to the directory of {@code filePath} and should be appropriately
     * overridden before performing any operations. If the path doesnt exist, a blank {@link Config}
     * will be created and saved to {@code filePath} when saving.
     *
     * @param filePath  path to the config file where remote details and document metadata will be saved
     * @return  Initialized Alexandria context instance that will be provided to operations
     * @throws IOException  problems converting strings to paths or general file loading problems
     */
    public static Context load(String filePath) throws IOException {
        Context context = new Context();
        Path path = Resources.path(filePath, false).toAbsolutePath();
        context.configPath(path);
        context.projectBase(path.getParent());
        context.searchPath(Collections.singletonList(path.getParent()));

        if(path.toFile().exists()) {
            Config originalConfig = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            Config config = Jackson.yamlMapper().readValue(path.toFile(), Config.class);
            context.config(config);
            context.originalConfig(originalConfig);
            log.debug(String.format("Loaded configuration from %s", path.toString()));
        } else{
            log.debug(String.format("Created default configuration for new file %s", path.toString()));
        }

        context.makePathsAbsolute();
        return context;
    }

    /**
     * Save the current context config (metadata and remote configuration) to disk.
     *
     * Not all information is saved, only the config field. See {@link Config} and {@link Context}.
     *
     * @param context  context containing configuration to save
     * @throws IOException  problems saving the file
     */
    public static void save(Context context) throws IOException {
        context.makePathsRelative();
        Config toSave = context.originalConfig;
        toSave.metadata(context.config.metadata());

        Jackson.yamlMapper().writeValue(context.configPath().toFile(), toSave);
        context.makePathsAbsolute();
        log.debug(String.format("Saved configuration to %s", context.configPath().toString()));
    }

    /**
     * Interface indicating that a class needs the Alexandria {@link Context}.
     *
     * This is mostly an informational class as there is no Dependency Injection framework to
     * automatically add context but {@link Context#configureRemote()} will examine the {@link Remote}
     * and {@link MarkdownConverter} classes to see if they implment this interface, providing the
     * {@link Context} to them if it is found.
     */
    public interface ContextAware {
        /**
         * Provides the {@link Context} to the implementing class.
         *
         * @param context  initialized Alexandria {@link Context}
         */
        void alexandriaContext(Context context);

        /**
         * Return the configured Alexandria {@link Context}.
         *
         * May return null if called before {@link ContextAware#alexandriaContext(Context)} has been called.
         * @return  Alexandria {@link Context}, nullable
         */
        Context alexandriaContext();
    }
}
