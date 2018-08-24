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

    private Alexandria alexandria = new Alexandria();

    public Alexandria init() throws IOException {
        if(getInputs() == null || getInputs().size() < 1){
            getInputs().add(rootDir());
        }
        if(getConfigPath() == null){
            setConfigPath(Paths.get(rootDir(), ".alexandria").toString());
        }
        getAlexandria().load(getConfigPath());
        getAlexandria().context().searchPath(getInputs());
        getAlexandria().context().output(Optional.of(getOutputPath()));
        if(getIncludes().size() > 0) {
            getAlexandria().context().include(getIncludes());
        }
        if(getExcludes().size() > 0) {
            getAlexandria().context().exclude(getExcludes());
        }
        return alexandria;
    }

    public void logContext(){
        getLog().debug("Alexandria - config file: " + getAlexandria().context().configPath());
        getLog().debug("Alexandria - project base dir: " + getAlexandria().context().projectBase());
        getLog().debug("Alexandria - inputs directories: " + getAlexandria().context().searchPath());
        getLog().debug("Alexandria - outputPath directory: " + getAlexandria().context().output());
        getLog().debug("Alexandria - includes files: " + getAlexandria().context().include());
        getLog().debug("Alexandria - excludes files: " + getAlexandria().context().exclude());
    }

    public boolean isExecutionRoot() {
        return getMavenSession().getExecutionRootDirectory().equalsIgnoreCase(getProject().getBasedir().toString());
    }

    public String rootDir(){
        MavenProject parent = getProject();
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
