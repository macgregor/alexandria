package com.github.macgregor.alexandria;

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

@Getter @Setter @Accessors(fluent = true)
public abstract class AlexandriaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @NonNull private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    @NonNull private MavenSession mavenSession;

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

    @Parameter( property = "alexandria.timeout", defaultValue = "30")
    private Integer timeout = 30;

    private Alexandria alexandria = new Alexandria();

    public Alexandria init() throws IOException {
        if(inputs == null || inputs.size() < 1){
            inputs.add(rootDir());
        }
        if(configPath == null){
            configPath = Paths.get(rootDir(), ".alexandria").toString();
        }
        alexandria.context(Context.load(Paths.get(configPath).toAbsolutePath().toString()));
        alexandria.context().searchPath(inputs.stream().map(Paths::get).collect(Collectors.toList()));
        alexandria.context().outputPath(Optional.of(Paths.get(outputPath)));
        alexandria.context().config().remote().requestTimeout(timeout);
        if(includes.size() > 0) {
            alexandria.context().include(includes);
        }
        if(excludes.size() > 0) {
            alexandria.context().exclude(excludes);
        }
        return alexandria;
    }

    public void logContext(){
        getLog().debug("Alexandria - config file: " + alexandria.context().configPath());
        getLog().debug("Alexandria - project base dir: " + alexandria.context().projectBase());
        getLog().debug("Alexandria - inputs directories: " + alexandria.context().searchPath());
        getLog().debug("Alexandria - outputPath directory: " + alexandria.context().outputPath());
        getLog().debug("Alexandria - includes files: " + alexandria.context().include());
        getLog().debug("Alexandria - excludes files: " + alexandria.context().exclude());
    }

    public boolean isExecutionRoot() {
        return mavenSession.getExecutionRootDirectory().equalsIgnoreCase(project.getBasedir().toString());
    }

    public String rootDir(){
        MavenProject parent = project;
        while(parent.getParent() != null){
            parent = parent.getParent();
        }
        return parent.getBasedir().toString();
    }
}
