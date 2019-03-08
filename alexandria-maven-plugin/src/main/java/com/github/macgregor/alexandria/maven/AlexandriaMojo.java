package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base class which handles most of the work of mapping Maven input parameters to an initialized Alexandria context.
 */
@Getter @Setter @Accessors(fluent = true)
public abstract class AlexandriaMojo extends AbstractMojo {

    /** Automatically set by Maven, user cannot override. */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @NonNull private MavenProject project;

    /** Automatically set by Maven, user cannot override. */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    @NonNull private MavenSession mavenSession;

    /**
     * Path to the Alexandria {@link Config} file to load/save to, generally .alexandria in the project execution
     * root directory.
     *
     * Maven Property: alexandria.configPath
     * Maps to: {@link Context#configPath}
     * Defaults to: {@link MavenSession#getExecutionRootDirectory()}/.alexandria (${session.executionRootDirectory}/.alexandria)
     */
    @Parameter( property = "alexandria.configPath")
    protected String configPath;

    /**
     * Path to output converted documents to. Generally target/alexandria
     *
     * Maven Property: alexandria.outputPath
     * Maps to: {@link Context#outputPath}
     * Defaults to: ${project.build.directory}/alexandria
     */
    @Parameter( property = "alexandria.outputPath", defaultValue = "${project.build.directory}/alexandria" )
    protected String outputPath;

    /**
     * Paths to use when searching for documents to index. By default its the project base dir meaning the entire project
     * repo will be searched.
     *
     * Maven Property: alexandria.inputs
     * Maps to: {@link Context#searchPath}
     * Defaults to: {@link MavenSession#getExecutionRootDirectory()} (${session.executionRootDirectory})
     */
    @Parameter( property = "alexandria.inputs")
    protected List<String> inputs = new ArrayList<>();

    /**
     * File include glob patterns to use when searching for documents to index. By default it will search for markdown files (*.md).
     *
     * For more details on acceptable patterns see:
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     * @see org.apache.commons.io.filefilter.WildcardFileFilter
     * @see PathFinder.GlobFileFilter
     * @see PathFinder.RelativeFileFilter
     *
     * Maven Property: alexandria.includes
     * Maps to: {@link Context#include}
     * Defaults to: Alexandria default ("*.md")
     */
    @Parameter( property = "alexandria.includes")
    protected List<String> includes = new ArrayList<>();

    /**
     * File exclude glob patterns to use when searching for documents to index. By default there are no exclude patterns,
     * so only the include patterns are used when matching files.
     *
     * For more details on acceptable patterns see:
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     * @see org.apache.commons.io.filefilter.WildcardFileFilter
     * @see PathFinder.GlobFileFilter
     * @see PathFinder.RelativeFileFilter
     *
     * Maven Property: alexandria.excludes
     * Maps to: {@link Context#exclude}
     * Defaults to: Alexandria default (none)
     */
    @Parameter( property = "alexandria.excludes")
    protected List<String> excludes = new ArrayList<>();

    /**
     * Timeout in seconds for each rest call to the remote.
     *
     * Maven Property: alexandria.timeout
     * Maps to: {@link com.github.macgregor.alexandria.Config.RemoteConfig#requestTimeout}
     * Defaults to: 30 seconds
     */
    @Parameter( property = "alexandria.timeout", defaultValue = "30")
    protected Integer timeout = 30;

    /**
     * Whether or not to fail the overall maven build if the plugin execution fails. If set to false, any errors
     * encountered will be logged at warning level.
     *
     * Maven Property: alexandria.failBuild
     * Defaults to: false
     */
    @Parameter( property = "alexandria.failBuild", defaultValue = "false")
    protected boolean failBuild = false;

    /**
     * Whether Alexandria should add a footer to all converted files warning readers they are not reading the
     * source document.
     *
     * Maven Property: alexandria.disclaimerFooterEnabled
     * Maps to: {@link Context#disclaimerFooterEnabled}
     * Defaults to: true
     */
    @Parameter( property = "alexandria.disclaimerFooterEnabled", defaultValue = "true")
    protected boolean disclaimerFooterEnabled = true;

    /**
     * Optional path to a custom (markdown) file to use as the footer added to documents.
     *
     * Maven Property: alexandria.disclaimerFooterPath
     * Maps to: {@link Context#disclaimerFooterPath}
     * Defaults to: null (use Alexandria default)
     */
    @Parameter( property = "alexandria.disclaimerFooterPath")
    protected String disclaimerFooterPath;

    private Alexandria alexandria = new Alexandria();

    /**
     * Initialize the Alexandria context from the Maven plugin parameters for implementing mojo classes to use.
     *
     * @return initialized {@link Context}
     * @throws IOException Alexandria metadata file cannot be loaded properly, (see {@link Context#load(String)}
     */
    public Alexandria init() throws IOException {
        if(inputs == null || inputs.isEmpty()){
            inputs = new ArrayList<>();
            inputs.add(rootDir());
        }
        if(configPath == null){
            configPath = Paths.get(rootDir(), ".alexandria").toString();
        }
        alexandria.context(Context.load(Paths.get(configPath).toAbsolutePath().toString()));
        alexandria.context().searchPath(inputs.stream().map(Paths::get).collect(Collectors.toList()));
        alexandria.context().outputPath(Optional.of(Paths.get(outputPath)));
        alexandria.context().config().remote().requestTimeout(timeout);
        alexandria.context().disclaimerFooterEnabled(disclaimerFooterEnabled);
        if(disclaimerFooterPath != null){
            alexandria.context().disclaimerFooterPath(Optional.of(Paths.get(disclaimerFooterPath)));
        }
        if(includes.size() > 0) {
            alexandria.context().include(includes);
        }
        if(excludes.size() > 0) {
            alexandria.context().exclude(excludes);
        }
        return alexandria;
    }

    /**
     * Debug logging of Maven plugin configuration.
     */
    public void logContext(){
        getLog().debug("Alexandria - config file: " + alexandria.context().configPath());
        getLog().debug("Alexandria - project base dir: " + alexandria.context().projectBase());
        getLog().debug("Alexandria - inputs directories: " + alexandria.context().searchPath());
        getLog().debug("Alexandria - outputPath directory: " + alexandria.context().outputPath());
        getLog().debug("Alexandria - disclaimer footer enabled : " + alexandria.context().disclaimerFooterEnabled());
        getLog().debug("Alexandria - disclaimer footer path : " +
                alexandria.context().disclaimerFooterPath()
                        .orElse(Paths.get("classpath:"+AlexandriaConvert.DEFAULT_DISCLAIMER_FOOTER_FILE)));
        getLog().debug("Alexandria - includes files: " + alexandria.context().include());
        getLog().debug("Alexandria - excludes files: " + alexandria.context().exclude());
    }

    /**
     * Test if the Maven plugin is being executed from the **local** root project directory, which can be important for
     * multimodule maven projects.
     *
     * If executed from a child project, rather than the parent, execution root will be false. This takes in to
     * consideration using remote parents (e.g. spring-boot-starters). This is meant to find the local base parent directory.
     *
     * @return true if executed from the local base project dir, false if executed in a child project.
     */
    public boolean isExecutionRoot() {
        return mavenSession.getExecutionRootDirectory().equalsIgnoreCase(project.getBasedir().toString());
    }

    /**
     * Walks the {@link MavenProject} to find the base dir. Note: I cant recall why I used this over
     * {@link MavenProject#getBasedir()}. Be sure to javadocs early, kids.
     *
     * @return
     */
    public String rootDir(){
        MavenProject parent = project;
        String dir = parent.getBasedir().toString();
        while(parent.getParent() != null){
            parent = parent.getParent();
            if(parent.getBasedir() != null){
                dir = parent.getBasedir().toString();
            }
        }
        return dir;
    }
}
