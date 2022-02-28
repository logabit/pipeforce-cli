# PIPEFORCE CLI

![build status](https://github.com/logabit/pipeforce-cli/actions/workflows/release.yml/badge.svg)

PIPEFORCE CLI is a command line interface which can be used to:

- Manage the PIPEFORCE Cloud remotely.
- Automate build processes.
- Develop integrations, apps, workflows and reports.
- Run and monitor tests.
- Automate and execute business workflows.

For more information, visit the [CLI reference documentation](https://pipeforce.github.io/docs/api/cli).

## Prerequisites

Install Java 8 or higher.

On Mac using Homebrew:

```
brew install java
```

Or [download](https://www.oracle.com/java/technologies/downloads/) and install Java manually.

## Installation

1. Download latest [pipeforce-cli.jar](https://github.com/logabit/pipeforce-cli/releases/latest).
2. Run this command in order to install it:

```
java -jar pipeforce-cli.jar setup
```

3. By default the installation location will be `$USER_HOME/pipeforce/pipeforce-cli`.
4. Add the `$USER_HOME/pipeforce/pipeforce-cli/bin/pi` script to your path variables of your operating system.

## Init a folder as app repo

In order to start developing your apps and to create VSCode workbench settings file plus the default structure to an
PIPEFORCE app folder, execute this command inside the folder:

```
> cd /my/app-repo
> pi init
```

## Update

The CLI will automatically detect newer versions and will ask to update itself if exists. But you can also trigger
manually such an update:

```
> pi update
```
**Note:** Only releases will be auto-updated. Release candidates need to be downloaded and installed manually.


:warning: **Warning**: If you're using a version <= 2.26 of the CLI (you can check that using `pi status`), then you need to delete/backup the folder $USER_HOME/pipeforce first and then follow the installation instruction above. Auto-update is no longer working for these older versions. The reason is because the auto-update, server and folder structure has changed fundamentally since then.

## Help

To display all options of the CLI, use this command:

```
> pi help
```

In order to get the description of a single pipeline command, use this:

```
> pi command <name-of-command>
```

In order to get a list of all available pipeline commands, use this:

```
> pi command
```

For more options and documentation about the CLI tool, please visit https://docs.pipeforce.io.

## Releasing the pipeforce-cli

### A stable version

To automatically build, test and create a stable release, set a tag of format: `v<version>-RELEASE` whereas `<version>`
must be the release version. Example:

```
git tag v3.0.0-RELEASE
git push origin --tags
```

Note: After a stable version was released, it is considered in all client-side installations as the latest version and
roll-out will be started automatically! So be careful in setting this tag!

### A release candidate (RC)

To automatically build, test and create a release draft **which is not considered in auto-rollout**, set a tag of
format: `v<version>-RC<number>` whereas `<version>` must be the designated release version and `<number>`
must be the number of the releae candidate. Example:

```
git tag v3.0.0-RC1
git push origin --tags
```
