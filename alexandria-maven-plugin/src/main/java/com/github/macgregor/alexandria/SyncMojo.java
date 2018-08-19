package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;

@Mojo( name = "sync", defaultPhase = LifecyclePhase.PACKAGE)
public class SyncMojo extends AlexandriaMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(isExecutionRoot()) {
            try {
                init();
                logContext();
                getAlexandria().syncWithRemote();
            } catch (IOException | BatchProcessException e) {
                throw new MojoFailureException("Failed to sync documents with remote.", e);
            }
        }
    }
}
