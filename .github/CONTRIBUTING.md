# Contributing

Contributions are welcome, especially adding new remotes so that others can make use of them. While this is currently a 
small personal project, I still have high standards for it and expect the same standards from contributors. The pull 
request template explains details but in short that means providing adequate unit test coverage and java docs.

## Naming Conventions
If you are making a new module, such as a new remove, please follow project naming conventions

module name: "alexandria-\*" or "alexandria-remote-\*" for remote implementations
maven group name: "com.github.macgregor"
maven artifact id: module name
maven artifact name: module name
package name: "com.github.macgregor.alexandria" or "com.github.macgregor.alexandria.remotes" for remote implementations

Otherwise follow standard java naming standards.

## Release Process
Releases are handled by Travis and must be triggered by myself (@macgregor). If your work involves some new repository
or service that needs to be added to the release process be let me know in advance and provide information on what I will
need to do to set an account up and integrate it into the CI process.