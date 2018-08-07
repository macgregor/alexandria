package com.github.macgregor.alexandria;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AlexandriaConvertMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSetOutputFromProjectProperty() throws Exception {
        File pom = generatePomWithProjectProperties(Collections.singletonMap("alexandria.output", "/foo"));
        final MavenProject mvnProject = new MavenProject() ;
        mvnProject.setFile( pom ) ;
        mvnProject.setVersion("1");
        mvnProject.getBuild().setDirectory(folder.newFolder("target").toString());
        mvnProject.getBuild().setOutputDirectory(folder.newFolder("output").toString());
        mvnProject.getBuild().setFinalName("test");
        mvnProject.setArtifact( new ProjectArtifact( mvnProject ));

        AlexandriaConvertMojo mojo = (AlexandriaConvertMojo)this.rule.lookupMojo( "convert", pom );
        this.rule.setVariableValueToObject( mojo, "project", mvnProject );
        assertThat(this.rule.getVariableValueFromObject(mojo, "output")).isEqualTo("/foo");
    }

    @Test
    public void testSetOutputFromPluginConfiguration() throws Exception {
        File pom = generatePomWithPluginConfiguration(Collections.singletonMap("alexandria.output", "/foo"));
        AlexandriaConvertMojo mojo = (AlexandriaConvertMojo) rule.lookupMojo("convert", pom);
        assertThat(mojo).isNotNull();
        assertThat(mojo.getOutput()).isEqualTo("/foo");
    }

    @Test
    public void testSetOutputDefault() throws Exception {
        File pom = generateDefaultPom();
        AlexandriaConvertMojo mojo = (AlexandriaConvertMojo) rule.lookupMojo("convert", pom);
        assertThat(mojo).isNotNull();
        assertThat(mojo.getOutput()).isEqualTo(Paths.get(pom.getParent(), "target", "alexandria").toString());
    }

    private File generatePomWithProjectProperties(Map<String, String> properties) throws IOException {
        List<String> propertyStrings = new ArrayList<>();
        for(Map.Entry entry : properties.entrySet()){
            propertyStrings.add(String.format("<%s>%s</%s>", entry.getKey(), entry.getValue(), entry.getKey()));
        }
        String content = "<project>\n" +
                        "   <properties>\n" +
                             String.join("\n", propertyStrings) +
                        "   </properties>\n" +
                        "   <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>junit</groupId>\n" +
                        "            <artifactId>junit</artifactId>\n" +
                        "            <version>4.12</version>\n" +
                        "            <scope>test</scope>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n"+
                        "    <build>\n" +
                        "        <plugins>\n" +
                            "        <plugin>\n" +
                            "            <groupId>org.apache.maven.plugins</groupId>\n" +
                            "            <artifactId>maven-plugin-plugin</artifactId>\n" +
                            "        </plugin>\n" +
                        "            <plugin>\n" +
                        "                <groupId>com.github.macgregor</groupId>\n" +
                        "                <artifactId>alexandria-maven-plugin</artifactId>\n" +
                        "                <version>0.0.1-SNAPSHOT</version>\n" +
                        "                <configuration>\n" +
                        "                </configuration>\n" +
                        "            </plugin>\n" +
                        "        </plugins>\n" +
                        "    </build>\n" +
                        "</project>";

        return generatePom(content);
    }

    private File generatePomWithPluginConfiguration(Map<String, String> properties) throws IOException {
        List<String> propertyStrings = new ArrayList<>();
        for(Map.Entry entry : properties.entrySet()){
            propertyStrings.add(String.format("<%s>%s</%s>", entry.getKey(), entry.getValue(), entry.getKey()));
        }
        String content = "<project>\n" +
                        "   <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>junit</groupId>\n" +
                        "            <artifactId>junit</artifactId>\n" +
                        "            <version>4.12</version>\n" +
                        "            <scope>test</scope>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n"+
                        "    <build>\n" +
                        "        <plugins>\n" +
                        "            <plugin>\n" +
                        "                <groupId>com.github.macgregor</groupId>\n" +
                        "                <artifactId>alexandria-maven-plugin</artifactId>\n" +
                        "                <version>0.0.1-SNAPSHOT</version>\n" +
                        "                <configuration>\n" +
                                             String.join("\n", propertyStrings) +
                        "                    <project implementation=\"com.github.macgregor.alexandria.AlexandriaProjectStub\"/>\n" +
                        "                </configuration>\n" +
                        "            </plugin>\n" +
                        "        </plugins>\n" +
                        "    </build>\n" +
                        "</project>";

        return generatePom(content);
    }

    private File generateDefaultPom() throws IOException {
        String content = "<project>\n" +
                        "   <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>junit</groupId>\n" +
                        "            <artifactId>junit</artifactId>\n" +
                        "            <version>4.12</version>\n" +
                        "            <scope>test</scope>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n"+
                        "    <build>\n" +
                        "        <plugins>\n" +
                        "            <plugin>\n" +
                        "                <groupId>com.github.macgregor</groupId>\n" +
                        "                <artifactId>alexandria-maven-plugin</artifactId>\n" +
                        "                <version>0.0.1-SNAPSHOT</version>\n" +
                        "                <configuration>\n" +
                        "                    <project implementation=\"com.github.macgregor.alexandria.AlexandriaProjectStub\"/>\n" +
                        "                </configuration>\n" +
                        "            </plugin>\n" +
                        "        </plugins>\n" +
                        "    </build>\n" +
                        "</project>";


        return generatePom(content);
    }

    private File generatePom(String pomContent) throws IOException {
        File pom = folder.newFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(pom));
        writer.write(pomContent);
        writer.close();
        return pom;
    }
}
