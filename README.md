# pipeforce-cli

PIPEFORCE CLI to manage the PIPEFORCE Cloud remotely.
![example workflow](https://github.com/logabit/pipeforce-cli/actions/workflows/release.yml/badge.svg)

## Prerequisites

Install Java 8 or higher.

On Mac using Homebrew:

```
brew install java
```

Or [download](https://www.oracle.com/java/technologies/downloads/) and install Java manually.

## Installation

1. Download latest [pipeforce-cli.jar](https://github.com/logabit/pipeforce-cli/releases/latest).
2. Run setup in order to install it: 

```
java -jar pipeforce-cli.jar setup
```

## Releasing

### Releasing a stable version

To automatically build, test and create a release, set a tag of format: `v<version>-RELEASE` whereas `<version>` must be
the release version. Example:

```
git tag v8.0.0-RELEASE
git push origin --tags
```

Note: After a stable version was released, it is considered in all client-side installations as the latest version and
roll-out will be started automatically! So be careful in setting this tag!

### Releasing a candidate

To automatically build, test and create a release draft **which is not considered in auto-rollout**, set a tag of
format: `v<version>-RC<number>` whereas `<version>` must be the designated release version and `<number>`
must be the number of the releae candidate. Example:

```
git tag v8.0.0-RC1
git push origin --tags
```
