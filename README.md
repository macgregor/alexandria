# Alexandria
##### Integrate markdown docs with antiquated html document hosting platforms.

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![BUILD](https://travis-ci.org/macgregor/alexandria.svg?branch=master)
<!---
![GitHub tag](https://img.shields.io/github/tag/expressjs/express.svg)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/https/oss.sonatype.org/com.google.guava/guava.svg)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.google.guava/guava.svg)
-->

Alexandria offers a way to keep your project documentation in markdown with your source code and deploy it to a
document hosting platform that doesnt support markdown as part of your normal project build and deploy process. This gives
many benefits including:
* use standard markdown, regardless of hosting platform requirements
* keep project documentation with the source
* use standard version control (e.g. git) for your documentation
* keep documentation up to date automatically as part of your build process
* decouple you from document hosting platforms

## Markdown to HTML Conversion
Alexandria looks for all markdown files in your project and converts them to html using [flexmark](https://github.com/vsch/flexmark-java).
It can be configured to look in multiple directories if needed.

## Metadata
Alexandria relies on metadata in the markdown files to know where to publish the convertedPath files. 

```markdown
<!--- alexandria
title: My Painstaking Documentation
remote: http://jive.corp.com/docs/remote-document-url
tags: tag1, tag2
createdOn: 2016-03-21T15:07:34.533+0000
lastUpdated: 2018-06-22T18:42:59.652+0000
-->
```

The plugin will update metadata as it runs, for example adding the remote url when it creates a new document. The
keys in the example above are standard metadata, but different remote implementations may need extra metadata. For example
the `JiveRemote` uses `jiveParentUrl` to add documents to a particular group, which is a Jive specific concept.

This means it is important to add a git commit as part of the deploy step to ensure metadata is persisted.


## Remotes
Remotes are hosting platforms to upload convertedPath files to. This will almost always be a rest interface for interacting 
with the platform's api.

## CLI
If you want to manually convert and upload files you can use the `alexandria-cli`. 

## Maven Plugin
The `alexandria-maven-plugin` can be used to add conversion/publishing to your maven project as part of the project build.