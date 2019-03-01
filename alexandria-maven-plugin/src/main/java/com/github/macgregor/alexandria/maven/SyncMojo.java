package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.AlexandriaSync;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;

/**
 * {@code mvn alexandria:sync}
 *
 * Executes {@link AlexandriaSync#syncWithRemote()} during the {@link LifecyclePhase#DEPLOY} phase
 */
@Mojo( name = "sync", defaultPhase = LifecyclePhase.DEPLOY)
public class SyncMojo extends AlexandriaMojo {

    /**
     * Initialized Alexandria context and executes {@link AlexandriaSync#syncWithRemote()}. It will only run if Maven is being
     * executed on the local project root directory, see {@link AlexandriaMojo#isExecutionRoot()}.
     *
     * @throws MojoExecutionException will never be thrown, but its on the interface
     * @throws MojoFailureException wraps any exception thrown, will only be thrown if {@link AlexandriaMojo#failBuild} is true
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(isExecutionRoot()) {
            try {
                init();
                logContext();
                alexandria().syncWithRemote();
            } catch (IOException e) {
                if(failBuild()) {
                    throw new MojoFailureException("Failed to sync documents with remote.", e);
                } else{
                    getLog().warn(e);
                }
            }
        }
    }
}
