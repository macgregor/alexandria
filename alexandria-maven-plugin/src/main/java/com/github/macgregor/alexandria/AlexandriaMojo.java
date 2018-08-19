package com.github.macgregor.alexandria;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AlexandriaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession mavenSession;

    @Parameter( property = "alexandria.config")
    private String configPath;

    @Parameter( property = "alexandria.output.directory", defaultValue = "${project.build.directory}/alexandria" )
    private String output;

    @Parameter( property = "alexandria.input.directory")
    private List<String> input = new ArrayList<>();

    @Parameter( property = "alexandria.input.include")
    private List<String> include = new ArrayList<>();

    @Parameter( property = "alexandria.input.exclude")
    private List<String> exclude = new ArrayList<>();

    public Context alexandriaContext() throws IOException {
        if(input == null || input.size() < 1){
            input.add(rootDir());
        }
        if(configPath == null){
            configPath = Paths.get(rootDir(), ".alexandria").toString();
        }
        Context context = Alexandria.load(configPath);
        context.searchPath(input);
        context.output(Optional.of(output));
        if(include.size() > 0) {
            context.include(include);
        }
        if(exclude.size() > 0) {
            context.exclude(exclude);
        }
        return context;
    }

    public void logConfig(Context context){
        getLog().info("Alexandria - config file: " + context.configPath());
        getLog().info("Alexandria - project base dir: " + context.projectBase());
        getLog().info("Alexandria - input directories: " + context.searchPath());
        getLog().info("Alexandria - output directory: " + context.output());
        getLog().info("Alexandria - include files: " + context.include());
        getLog().info("Alexandria - exclude files: " + context.exclude());
    }

    protected boolean isExecutionRoot() {
        return mavenSession.getExecutionRootDirectory().equalsIgnoreCase(project.getBasedir().toString());
    }

    protected String rootDir(){
        MavenProject parent = project;
        while(parent.getParent() != null){
            parent = parent.getParent();
        }
        return parent.getBasedir().toString();
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }
}
