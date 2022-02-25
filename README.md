# pipeforce-cli

PIPEFORCE CLI to manage the PIPEFORCE Cloud remotely.

## Installation

### MacOS

```
brew tap logabit/pipeforce
brew install pipeforce-cli
```

## Releasing

### Setting a release tag via Git
For example:

```
git tag v1.2.3-RC2
git push origin --tags
```

### Releasing a stable version

To automatically build, test and create a release, set a tag of format: `v<version>-RELEASE` whereas `<version>` must be
the release version. Example:

```
v8.0.0-RELEASE
```

Note: After a stable version was released it is considered in all client-side installations as the latest version and
roll-out will be started automatically! So be careful in setting this tag!

### Releasing a candidate

To automatically build, test and create a release draft **which is not considered in auto-rollout**, set a tag of
format: `v<version>-RC<number>` whereas `<version>` must be the designated release version and `<number>`
must be the number of the releae candidate. Example:

```
v8.0.0-RC1
```