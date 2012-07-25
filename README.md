# SVN Migration Scripts

## Building the JAR

To build the JAR, you need a JDK and [SBT][]. You can [download a JDK][] from Oracle; go for Java 6 or newer. See [this page][install SBT] for installation instructions for SBT.

Once you've installed Java & SBT, you can create the JAR by running (from the root of the project):

    $ sbt assembly

This will create the JAR file under the `target/` directory.

## Running the scripts

Having built the JAR, you can run the scripts it contains. All commands have the format:

    $ java -jar svn-migration-scripts.jar <command> <args>…

A list of available commands can be obtained by omitting the command and arguments (e.g., `java -jar svn-migration-scripts.jar`).

You need the following dependencies:

* A Java runtime, version 6 or newer ([download from Oracle][download a JDK]),
* [Git][], with `git-svn` included, version 1.7.7.5 or newer, and
* [Subversion][], version 1.6.17 or newer.

### clean-git

This command cleans up a Git repository created with `git-svn`. It creates annotated Git tags corresponding to the Subversion tags detected, creates local branches corresponding to the Subversion branches, and removes any branches or tags which do not currently exist in Subversion (but may have, for example, existed in the past). It also attempts to tidy up tag/branch names which are not allowable in Git.

The command is run as follows:

    $ java -jar svn-migration-scripts.jar clean-git http://repository.example.org/svn

### authors

This command prints to standard output a list of the user names that have committed to a Subversion repository, in the format of an example author mapping. It is run as follows:

    $ java -jar svn-migration-scripts.jar authors http://repository.example.org/svn [username [password]]

If the Subversion repository requires you to authenticate against it, you can specify a user name and optionally a password. If a user name is specified and a password is omitted, you will be interactively prompted for a password.

The output file is of the format:

    j.doe = j.doe <j.doe@mycompany.com>
    …

### authors-ondemand

There is another version of the `authors` command available if you're migrating from Atlassian OnDemand. You can instead run:

    $ java -jar svn-migration-scripts.jar authors-ondemand your-instance.atlassian.net username password

This time, the username and password are both required. This generates an example author mapping, as with the `authors` command, however this command tries to look up user details from your OnDemand instance. For example, rather than the file created above, it might create instead:

    j.doe = Jane Doe <jane@some.company.example.com>
    …

#### Subversion permissions

For either of these commands (`authors` and `authors-ondemand`) to succeed when run against an OnDemand instance, you may need to adjust Subversion permissions to grant the user whose credentials you use permission to read the root of the Subversion file system. This permission is not granted by default. Information on granting repository permissions at the path level in Atlassian OnDemand is [available in the OnDemand documentation][SVN permissions]; the path that should be configured is `/`.

[SBT]: https://github.com/harrah/xsbt/wiki/
[download a JDK]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[install SBT]: https://github.com/harrah/xsbt/wiki/Getting-Started-Setup
[Git]: http://git-scm.com/
[Subversion]: http://subversion.apache.org/
[SVN permissions]: https://confluence.atlassian.com/display/AOD/Configuring+repository+permissions+for+a+project#Configuringrepositorypermissionsforaproject-Configuringrepositorypermissionsatthepathlevel
