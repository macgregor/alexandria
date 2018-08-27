# Alexandria
##### Integrate markdown docs with antiquated html document hosting platforms using standard build tools.

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub version](https://badge.fury.io/gh/macgregor%2Falexandria.svg)](https://badge.fury.io/gh/macgregor%2Falexandria)
[![Build Status](https://travis-ci.com/macgregor/alexandria.svg?branch=master)](https://travis-ci.com/macgregor/alexandria)
[![Coverage Status](https://coveralls.io/repos/github/macgregor/alexandria/badge.svg?branch=master)](https://coveralls.io/github/macgregor/alexandria?branch=master)
[![Maintainability](https://api.codeclimate.com/v1/badges/9d5c11a22e7e1d53693e/maintainability)](https://codeclimate.com/github/macgregor/alexandria/maintainability)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.macgregor/alexandria-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.macgregor%22%20AND%20a:%22alexandria-maven-plugin%22)
<!---
![GitHub tag](https://img.shields.io/github/tag/expressjs/express.svg)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/https/oss.sonatype.org/com.google.guava/guava.svg)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.google.guava/guava.svg)
-->

Alexandria bring CI concepts to document hosting platforms which dont integrate with the tools and processes 
we have grown reliant on. Keep your documents with your source code in friendly markdown and let Alexandria convert and
sync them as part of your project's build process. 

Any platform which offers a rest API can be added to the project by
implementing an interface for creating, updating and deleting documents. Alexandria handles the rest.

```text
# mvn deploy
[INFO] Scanning for projects...
[INFO]
[INFO] ----------------< com.github.macgregor:alexandria-demo >----------------
[INFO] Building alexandria-demo 0.0.6-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- alexandria-maven-plugin:0.0.6-SNAPSHOT:index (alexandria) @ alexandria-demo ---
[INFO] Matched 4 files (1 indexed, 3 already indexed)
[INFO]
[INFO] --- alexandria-maven-plugin:0.0.6-SNAPSHOT:convert (alexandria) @ alexandria-demo ---
[INFO] 4 out of 4 files converted successfully.
[INFO]
[INFO] --- alexandria-maven-plugin:0.0.6-SNAPSHOT:sync (alexandria) @ alexandria-demo ---
[INFO] images.md is already up to date (checksum: 1751689934, last updated: 2018-08-22T02:27:51.789Z)
[INFO] Update document at empahsis.md https://jive.com/docs/DOC-1140809-empahsismd
[INFO] Update document at codeblocks.md https://jive.com/docs/DOC-1140806-codeblocksmd
[INFO] Created new document at https://jive.com/docs/DOC-1140819
[INFO] Synced 4 out of 4 documents with remote https://jive.com/api/core/v3
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.118 s
[INFO] Finished at: 2018-08-21T23:02:07-04:00
[INFO] ------------------------------------------------------------------------
```

## Requirements
* maven 3.5.2 or greater (for running alexandria-maven-plugin only)
* Java 8

## Getting Started
1. Check out the [javadocs](https://macgregor.github.io/alexandria/)
2. After you've ignored that, run `mvn alexandria:index` or `java -jar alexandria-cli.jar index` to generate a config file ([.alexandria](./alexandria-demo/.alexandria) by default)
2. add remote url, username, password and update metadata index if necessary (username and password can be set to an envrionment/system variable like `${env.foo}`)
3. run `mvn deploy` or `java -jar alexandria-cli.jar`

See [alexandria-demo](./alexandria-demo) for a working example of the maven plugin. 

```xml
<plugin>
    <groupId>com.github.macgregor</groupId>
    <artifactId>alexandria-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>alexandria</id>
            <goals>
                <goal>index</goal>
                <goal>convert</goal>
                <goal>sync</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Concepts

### Remotes
Remotes are hosting platforms to upload convertedPath files to. This will almost always be a rest interface for interacting 
with the platform's api. Remotes included with the release include:
* **NoOp Remote** - default remote that can be used to run Alexandria without actually uploading to a remote, which can be
useful for testing.
* **Jive Remote** - uses the [Jive rest api](https://developers.jivesoftware.com/api/v3/cloud/rest/index.html) to upload
documents to a Jive document platform.
  
The remote used is set and configured in the Alexandria config file.
```yaml
remote:
  baseUrl: "https:/jive.com/api/core/v3"
  username: "macgregor"
  password: "password"
  class: "com.github.macgregor.alexandria.remotes.JiveRemote"
```

New remotes can be added by implementing the `Remote` interface which defines methods that will be called to create, update
or delete a document with the remote.

### Alexandria Lifecycle: index -> convert -> sync
Each of these phases can be run independently and reran without any issue. Both `mvn deploy` and `java -jar alexandria-cli.jar`
will run all three in order. In general errors are collected as they occur and thrown in a batch at the end of a lifecycle
phase so that a problem in 1 file wont interfere with others that are fine.

#### Index
Alexandria generates and stores metadata about your documents. The indexing phase finds documents and adds them to the
index file for later use. You can also create or modify this index by hand, for example if the document already exists on 
the remote you may want to specify the `remoteUri` so the file is updated and not recreated.

#### Convert
The convert phase uses the existing metadata index to convert files from markdown to html using [flexmark](https://github.com/vsch/flexmark-java)
to do the heavy lifting. If your remote supports native markdown, you can set `supportsNativeMarkdown` and the conversion
phase will be skipped and sync will upload the source markdown file.

#### Sync
This is where most of the complexity is. 
* **create** - if no `remoteUri` is set in the metadata, create the deocument
* **update** - if `remoteUri` is set in the metadata and the runtime `sourceChecksum` of the file is different than the stored 
`sourceChecksum` (or `sourceChecksum` is not set), update the document
* **delete** - not implemented yet

## Contributing
Contributions are welcome, especially adding new remotes so that others can make use of them. Please provide adequate unit
tests as part of the pull request.

## Trobleshooting
* [crazy long error about eclipse loggers](https://stackoverflow.com/questions/47920305/can-not-set-org-eclipse-aether-spi-log-logger-with-custom-maven-plugin) when trying to install alexandria-maven-plugin. Upgrade maven to at least 3.5.2

## Links
* [Javadocs](https://macgregor.github.io/alexandria/)
* [Travis Build](https://travis-ci.com/macgregor/alexandria)
* [Code Climate](https://codeclimate.com/github/macgregor/alexandria)
* [Coveralls](https://coveralls.io/github/macgregor/alexandria)
* [Maven Artifacts](https://search.maven.org/search?q=g:com.github.macgregor%20AND%20a:alexandria-*)