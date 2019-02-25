package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.AlexandriaIndex;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * {@code mvn alexandria:index}
 *
 * Executes {@link AlexandriaIndex#update()} during the {@link LifecyclePhase#PREPARE_PACKAGE} phase
 */
@Mojo( name = "index", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class IndexMojo extends AlexandriaMojo {

    /**
     * Initialized Alexandria context and executes {@link AlexandriaIndex#update()}. It will only run if Maven is being
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
                alexandria().index();
            } catch (Exception e) {
                if(failBuild()) {
                    throw new MojoFailureException("Failed to index documents.", e);
                } else{
                    getLog().warn(e);
                }
            }
        }
    }
}
