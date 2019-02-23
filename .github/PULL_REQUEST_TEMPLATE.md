## Pull Request template
Please, go through these steps before you submit a PR.

First, there must be an issue associated with your work so it can be tracked for releases. If you dont have an issue 
please stop and open one now.

Make sure that:
1. There is an issue associated with your PR (see above). 
2. Your changes are in a separate branch, with a descriptive name.
3. You have a descriptive commit message with a short title (first line).
4. You have only one commit (if not, squash them into one commit).
5. All tests are passing and Code coverage has not dropped below current master level (>= 95%): 
`mvn clean verify && open alexandria-reports/target/site/jacoco-aggregate/index.html`
6. Your code has adequate java docs and you checked that they build and look ok: 
`mvn deploy -PcreateJavadocs -Dmaven.test.skip=true && open alexandria-core/target/apidocs/index.html`
7. You have reasonably paranoid error handling for your code

**After** these steps you can open a pull request:
1. Give a descriptive title to your PR.
2. Provide a description of your changes.
3. Put `closes #XXXX` (or [similar](https://help.github.com/en/articles/closing-issues-using-keywords)) in your comment 
to auto-close the issue that your PR fixes (if such).

IMPORTANT: Please review the [CONTRIBUTING.md](./CONTRIBUTING.md) file for detailed contributing guidelines.

**PLEASE REMOVE THIS TEMPLATE BEFORE SUBMITTING**