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

    @Parameter( property = "alexandria.configPath")
    private String configPath;

    @Parameter( property = "alexandria.outputPath", defaultValue = "${project.build.directory}/alexandria" )
    private String outputPath;

    @Parameter( property = "alexandria.inputs")
    private List<String> inputs = new ArrayList<>();

    @Parameter( property = "alexandria.includes")
    private List<String> includes = new ArrayList<>();

    @Parameter( property = "alexandria.excludes")
    private List<String> excludes = new ArrayList<>();

    private Alexandria alexandria;

    public Alexandria init() throws IOException {
        if(inputs == null || inputs.size() < 1){
            inputs.add(rootDir());
        }
        if(configPath == null){
            configPath = Paths.get(rootDir(), ".alexandria").toString();
        }
        alexandria = new Alexandria();
        alexandria.load(configPath);
        alexandria.context().searchPath(inputs);
        alexandria.context().output(Optional.of(outputPath));
        if(includes.size() > 0) {
            alexandria.context().include(includes);
        }
        if(excludes.size() > 0) {
            alexandria.context().exclude(excludes);
        }
        return alexandria;
    }

    public void logContext(){
        getLog().info("Alexandria - config file: " + alexandria.context().configPath());
        getLog().info("Alexandria - project base dir: " + alexandria.context().projectBase());
        getLog().info("Alexandria - inputs directories: " + alexandria.context().searchPath());
        getLog().info("Alexandria - outputPath directory: " + alexandria.context().output());
        getLog().info("Alexandria - includes files: " + alexandria.context().include());
        getLog().info("Alexandria - excludes files: " + alexandria.context().exclude());
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

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
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
