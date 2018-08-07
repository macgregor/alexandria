package com.github.macgregor.alexandria;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mojo( name = "convert")
public class AlexandriaConvertMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter( property = "alexandria.output", defaultValue = "${project.build.directory}/alexandria" )
    private String output;

    @Parameter( property = "alexandria.overwrite", defaultValue = "true" )
    private boolean overwrite;

    @Parameter( property = "alexandria.input")
    private List<String> input = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(input == null || input.size() < 1){
            input.add(project.getBasedir().toString());
        }

        getLog().info("Alexandria output dir: " + output);
        getLog().info("Alexandria input directories: " + input);
        getLog().info("Alexandria overwrite generated files: " + overwrite);
        try {
            MarkdownConverter converter = new MarkdownConverter(input, Optional.ofNullable(output), Optional.of(overwrite));
            List<DocumentMetadata> converted = converter.convert();
            for(DocumentMetadata m : converted){
                getLog().info(String.format("Created %s", m.getConverted().get().toString()));
            }
        } catch(IOException | URISyntaxException e){
            throw new MojoFailureException("Failed to convert documents.", e);
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }
}
