package com.github.macgregor.alexandria.maven;

import com.github.macgregor.alexandria.AlexandriaConvert;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * {@code mvn alexandria:convert}
 *
 * Executes {@link AlexandriaConvert#convert()} during the {@link LifecyclePhase#PACKAGE} phase
 */
@Mojo( name = "convert", defaultPhase = LifecyclePhase.PACKAGE)
public class ConvertMojo extends AlexandriaMojo {

    /**
     * Initialized Alexandria context and executes {@link AlexandriaConvert#convert()}. It will only run if Maven is being
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
