package com.github.macgregor.alexandria;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AlexandriaProjectStub extends MavenProjectStub {
    /**
     * Default constructor
     */
    public AlexandriaProjectStub() {

        setGroupId("testGroup");
        setArtifactId("testArtifact");
        setVersion("1.0-SNAPSHOT");
        setName("test");
        setUrl("www.google.com");
        setPackaging("jar");
        setModelVersion("4.0.0");

        Build build = new Build();
        build.setFinalName("testArtifact");
        build.setDirectory(getBasedir() + "/target");
        build.setSourceDirectory(getBasedir() + "/src/main/java");
        build.setOutputDirectory(getBasedir() + "/target/classes");
        build.setTestSourceDirectory(getBasedir() + "/src/test/java");
        build.setTestOutputDirectory(getBasedir() + "/target/test-classes");
        setBuild(build);

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add(getBasedir() + "/src/main/java");
        setCompileSourceRoots(compileSourceRoots);

        List testCompileSourceRoots = new ArrayList();
        testCompileSourceRoots.add(getBasedir() + "/src/test/java");
        setTestCompileSourceRoots(testCompileSourceRoots);
    }

    /**
     * {@inheritDoc}
     */
    public File getBasedir() {
        return super.getBasedir();
    }
}