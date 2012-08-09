package com.atlassian.svn2git

import org.apache.commons.io.FileUtils

object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean, stripMetadata: Boolean)

  val name = "clean-git"
  override val usage = Some("[--force] [--no-delete] [--strip-metadata] ...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    Right(arguments.partition(_ startsWith "-"))
  }

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]): Boolean = {
    import cmd._
    git.ensureRootGitDirExists()
    val (branches, tags) = getSVNRoots(cmd)
    val force = options.contains("--force")
    implicit val opts = Options(force, force && !options.contains("--no-delete"), options.contains("--strip-metadata"))
    if (!force) {
      println(
        """###########################################################
          |#                  This is a dry run                      #
          |#         No changes will be made to your repository      #
          |###########################################################""".stripMargin)
    }

    Tags.annotate(cmd)
    Branches.createLocal(cmd)
    def checkObsolete(a: Array[String], f: Array[String] => Unit) = {
      if (a.find(_.contains("*")).isEmpty) {
        f(a)
      } else {
        println("WARNING: Non-standard SVN branch/tag configuration, could not clean.")
      }
    }
    checkObsolete(tags, Tags.checkObsolete(cmd))
    checkObsolete(branches, Branches.checkObsolete(cmd))
    Tags.fixNames(cmd)
    Branches.fixNames(cmd)

    stripMetadata(cmd)
    warnIfLargeRepository(cmd)
  }

  def getSVNRoots(cmd: Cmd) = {
    def getConfig(s: String) = cmd.git("git config --get-all svn-remote.%s.%s".format("svn", s)).lines
    val url = getConfig("url").headOption.getOrElse("")
    def splitRefSpec(s: Stream[String]) = s.map(_.split("\\:")(0).replaceAll("\\*$", "")).toArray
    def getRefSpec(s: String) = splitRefSpec(getConfig(s)).map(safeURLAppend(url, _))
    (getRefSpec("branches"), getRefSpec("tags"))
  }

  def stripMetadata(cmd: Cmd)(implicit options: Clean.Options) = {
    import java.io.File
    import cmd._

    if (options.stripMetadata) {
      if ((git.dir /: Seq("info", "grafts"))(new File(_, _)).exists) {
        println("ERROR: Metadata not stripped: grafts exist.")
        true
      } else if (Option((git.dir /: Seq("refs", "replace"))(new File(_, _)).listFiles).getOrElse(Array()).nonEmpty) {
        println("ERROR: Metadata not stripped: replacement refs exist.")
        true
      } else {
        println("# removing Subversion metadata from Git commit messages")
        if (options.shouldDelete) {
          git("git", "filter-branch", "--msg-filter", "sed -e '/^git-svn-id:/d'").!
        }
        false
      }
    } else false
  }

  def warnIfLargeRepository(cmd: Cmd) = {
    val size = FileUtils.sizeOfDirectory(cmd.git.dir)
    if (size > (1 << 30)) {
      println("Warning: your repository is larger than 1GB.")
      println("See https://dvcsroute.atlassian.net/wiki/x/XQAQ on how to reduce the size of your repository.")
      false
    } else true
  }
}
