package com.github.macgregor.alexandria;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AlexandriaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter( property = "alexandria.config", defaultValue = "${project.basedir}/.alexandria")
    private String configPath;

    @Parameter( property = "alexandria.output.directory", defaultValue = "${project.build.directory}/alexandria" )
    private String output;

    @Parameter( property = "alexandria.input.directory")
    private List<String> input = new ArrayList<>();

    @Parameter( property = "alexandria.input.include")
    private List<String> include = new ArrayList<>();

    @Parameter( property = "alexandria.input.exclude")
    private List<String> exclude = new ArrayList<>();

    public Config alexandriaConfig() throws IOException {
        if(input == null || input.size() < 1){
            input.add(project.getBasedir().toString());
        }
        Config config = Config.load(configPath);
        config.searchPath(input);
        config.output(Optional.of(output));
        if(include.size() > 0) {
            config.include(include);
        }
        if(exclude.size() > 0) {
            config.exclude(Optional.of(exclude));
        }
        return config;
    }

    public void logConfig(Config config){
        getLog().info("Alexandria - config file: " + config.configPath());
        getLog().info("Alexandria - input directories: " + config.searchPath());
        getLog().info("Alexandria - output directory: " + config.output());
        getLog().info("Alexandria - include files: " + config.include());
        getLog().info("Alexandria - exclude files: " + config.exclude());
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
