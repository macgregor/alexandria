package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;

@Mojo( name = "convert", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ConvertMojo extends AlexandriaMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try{
            Context context = alexandriaContext();
            logConfig(context);
            Alexandria.convert(context);
        } catch (IOException | BatchProcessException e){
            throw new MojoFailureException("Failed to convert documents.", e);
        }
    }
}
