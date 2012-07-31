# SVN Migration Scripts

## Building the JAR

To build the JAR, you need a JDK and [SBT][]. You can [download a JDK][] from Oracle; go for Java 6 or newer. See [this page][install SBT] for installation instructions for SBT.

Once you've installed Java & SBT, you can create the JAR by running (from the root of the project):

    $ sbt assembly

This will create the JAR file under the `target/` directory.

## Running the scripts

Having built the JAR, you can run the scripts it contains. All commands have the format:

    $ java -jar svn-migration-scripts.jar <command> <args>…

A list of available commands can be obtained by omitting the command and arguments (e.g., `java -jar svn-migration-scripts.jar`). All commands optionally take a `--help` argument, displaying usage information about the command.

You need the following dependencies:

* A Java runtime, version 6 or newer ([download from Oracle][download a JDK]),
* [Git][], with `git-svn` included, version 1.7.7.5 or newer, and
* [Subversion][], version 1.6.17 or newer.

#### A note on Subversion permissions

Many of these commands will access your Subversion repository to gather information. In particular, `authors` needs to be run as a user with read access to your entire Subversion tree. If you are using Atlassian OnDemand, by default, no users have read access to the root of the Subversion tree, and as such you will need to grant read access to the user whose credentials you are using for the conversion process. You can read [our documentation on configuring repository permissions at the path level in OnDemand][SVN permissions]; the path that needs to be configured is `/`.

### verify

This command will perform some simple tests to ensure that your system has the required dependencies to convert your Subversion repository to Git. In particular, it checks whether you have sufficiently recent versions of:

* Subversion
* Git
* `git-svn`

It also tests whether you're running the command on a case-insensitive file-system. We do not support running conversions with `git-svn` on a case-insensitive file-system; they can in some cases lead to corrupted conversions.

> TODO: How can you tell that corruption has occurred? How can this be avoided e.g. instructions for creating/mounting a case-sensitive disk image on OS X.

### clean-git

This command cleans up a Git repository created with `git-svn`. It creates annotated Git tags corresponding to the Subversion tags detected, creates local branches corresponding to the Subversion branches, and removes any branches or tags which do not currently exist in Subversion (but may have, for example, existed in the past). It also attempts to tidy up tag/branch names which are not allowable in Git.

The command is run as follows:

    $ java -jar svn-migration-scripts.jar clean-git [--dry-run] [--no-delete] [--strip-metadata] http://repository.example.org/svn

If the `--dry-run` option is specified, the command will not perform any actions, but will instead simply show what would be done. If the `--no-delete` option is specified, branches and tags will be created but none will be removed. If the `--strip-metadata` option is specified, the infomartion in Git commit messages created by `git-svn` specifiying the Subversion revision corresponding to the Git commit will be removed.

### authors

This command prints to standard output a list of the user names that have committed to a Subversion repository, in the format of an example author mapping. It is run as follows:

    $ java -jar svn-migration-scripts.jar authors http://repository.example.org/svn [username [password]]

If the Subversion repository requires you to authenticate against it, you can specify a user name and optionally a password. If a user name is specified and a password is omitted, you will be interactively prompted for a password.

The output file is of the format:

    j.doe = j.doe <j.doe@mycompany.com>
    …

Once the initial file has been generated *it is important to edit it so that it contains the **full names** and e-mail addresses of the committers to the Subversion repository*. If this is not done, commits may not be associated with the appropriate users when you push your repository to Bitbucket. For example, you might edit the file generated above to read

    j.doe = Jane Doe <jane@somecompany.example>
    …

#### The authors command in OnDemand

If you run the authors command against an Atlassian OnDemand instance, the command will attempt to look up the full name and e-mail address of each committer in JIRA. If the command is unable to find a corresponding JIRA user for a Subversion committer, the username will be present in the generated authors list without any mapping. *You will need to edit the authors list to supply a valid mapping for such users before you can use the authors file in a conversion*. All such users will appear at the beginning of the generated authors file.

[SBT]: https://github.com/harrah/xsbt/wiki/
[download a JDK]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[install SBT]: https://github.com/harrah/xsbt/wiki/Getting-Started-Setup
[Git]: http://git-scm.com/
[Subversion]: http://subversion.apache.org/
[SVN permissions]: https://confluence.atlassian.com/display/AOD/Configuring+repository+permissions+for+a+project#Configuringrepositorypermissionsforaproject-Configuringrepositorypermissionsatthepathlevel
