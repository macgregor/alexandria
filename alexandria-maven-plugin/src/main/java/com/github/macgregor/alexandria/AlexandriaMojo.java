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

    @Parameter( property = "alexandria.output", defaultValue = "${project.build.directory}/alexandria" )
    private String output;

    @Parameter( property = "alexandria.input")
    private List<String> input = new ArrayList<>();

    @Parameter( property = "alexandria.include")
    private List<String> include = new ArrayList<>();

    @Parameter( property = "alexandria.exclude")
    private List<String> exclude = new ArrayList<>();

    private Alexandria alexandria;

    public Alexandria init() throws IOException {
        if(input == null || input.size() < 1){
            input.add(rootDir());
        }
        if(configPath == null){
            configPath = Paths.get(rootDir(), ".alexandria").toString();
        }
        alexandria = new Alexandria();
        alexandria.load(configPath);
        alexandria.context().searchPath(input);
        alexandria.context().output(Optional.of(output));
        if(include.size() > 0) {
            alexandria.context().include(include);
        }
        if(exclude.size() > 0) {
            alexandria.context().exclude(exclude);
        }
        return alexandria;
    }

    public void logContext(){
        getLog().info("Alexandria - config file: " + alexandria.context().configPath());
        getLog().info("Alexandria - project base dir: " + alexandria.context().projectBase());
        getLog().info("Alexandria - input directories: " + alexandria.context().searchPath());
        getLog().info("Alexandria - output directory: " + alexandria.context().output());
        getLog().info("Alexandria - include files: " + alexandria.context().include());
        getLog().info("Alexandria - exclude files: " + alexandria.context().exclude());
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

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    public void setMavenSession(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    public Alexandria getAlexandria() {
        return alexandria;
    }

    public void setAlexandria(Alexandria alexandria) {
        this.alexandria = alexandria;
    }
}
