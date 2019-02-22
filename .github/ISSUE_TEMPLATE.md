## Expected Behavior


## Actual Behavior

Include full stack traces and Alexandria debug output when available:
* cli: `java -jar alexandria-cli.jar -vvv [convert|index|sync]`
* maven plugin: `mvn -X alexandria:index alexandria:convert alexandria:sync`
* alexandria as dependency: set com.github.macgregor.alexandria to debug, e.g. 
`<logger name="com.github.macgregor.alexandria" level="debug" />` (logback.xml)


## Steps to Reproduce the Problem

  1.
  2.
  3.

## Specifications

| | |
| --- | --- |
| Alexandria Version | 0.1.4 |
| Java Version | 1.8.0_121 |
| Maven Version | 3.6.0 |
| Platform | OSX Mojave |
| Component | alexandria-maven-plugin |

Please also provide a link to your git repo containing or attach/provide:
* project (file system) structure 
* .alexandria
* debug/verbose output from alexandria:
  * cli - `java -jar alexandria-cli.jar -vvv [convert|index|sync]`
  * maven plugin - `mvn -X alexandria:index alexandria:convert alexandria:sync`
  * alexandria as dependency: set com.github.macgregor.alexandria to debug, e.g. 
  `<logger name="com.github.macgregor.alexandria" level="debug" />` (logback.xml)
* pom.xml (if relevant)
* alexandria cli parameters (if relevant)
* markdown/html documents (if relevant)

**BE SURE TO REDACT SENSITIVE DATA** 