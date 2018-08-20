# Alexandria
##### Integrate markdown docs with antiquated html document hosting platforms.

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![BUILD](https://travis-ci.org/macgregor/alexandria.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/macgregor/alexandria/badge.svg?branch=master)](https://coveralls.io/github/macgregor/alexandria?branch=master)
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

## Requirements
* maven 3.5.2 or greater
* Java 8

## Markdown to HTML Conversion
Alexandria looks for all markdown files in your project and converts them to html using [flexmark](https://github.com/vsch/flexmark-java).
It can be configured to look in multiple directories if needed.

## Remotes
Remotes are hosting platforms to upload convertedPath files to. This will almost always be a rest interface for interacting 
with the platform's api.

## CLI
If you want to manually convert and upload files you can use the `alexandria-cli`. 

## Maven Plugin
The `alexandria-maven-plugin` can be used to add conversion/publishing to your maven project as part of the project build.

## Trobleshooting
* [crazy long error about eclipse loggers](https://stackoverflow.com/questions/47920305/can-not-set-org-eclipse-aether-spi-log-logger-with-custom-maven-plugin) when trying to install alexandria-maven-plugin. Upgrade maven to at least 3.5.2