package com.github.macgregor.alexandria;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "convert", defaultPhase = LifecyclePhase.PACKAGE)
public class ConvertMojo extends AlexandriaMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(isExecutionRoot()) {
            try {
                init();
                logContext();
                alexandria().convert();
            } catch (Exception e) {
                if(failBuild()) {
                    throw new MojoFailureException("Failed to convert documents.", e);
                } else{
                    getLog().warn(e);
                }
            }
        }
    }
}
