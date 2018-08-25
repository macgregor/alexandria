package com.github.macgregor.alexandria;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;

@Mojo( name = "index", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class IndexMojo extends AlexandriaMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(isExecutionRoot()) {
            try {
                init();
                logContext();
                alexandria().index();
            } catch (IOException e) {
                throw new MojoFailureException("Failed to index documents.", e);
            }
        }
    }
}
