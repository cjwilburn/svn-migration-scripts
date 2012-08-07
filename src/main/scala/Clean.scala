package com.atlassian.svn2git

import sys.process._
import java.io.File

object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean, stripMetadata: Boolean)

  val name = "clean-git"
  override val usage = Some("[--force] [--no-delete] [--strip-metadata] ...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    Right(arguments.partition(_ startsWith "-"))
  }

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    Git.ensureRootGitDirExists()
    val (branches, tags) = getSVNRoots()
    val force = options.contains("--force")
    implicit val opts = Options(force, force && !options.contains("--no-delete"), options.contains("--strip-metadata"))
    if (!force) {
      println(
        """###########################################################
          |#                  This is a dry run                      #
          |#         No changes will be made to your repository      #
          |###########################################################""".stripMargin)
    }

    Tags.annotate()
    Branches.createLocal()
    def checkObsolete(a: Array[String], f: Array[String] => Unit) = {
      if (a.find(_.contains("*")).isEmpty) {
        f(a)
      } else {
        println("WARNING: Non-standard SVN branch/tag configuration, could not clean.")
      }
    }
    checkObsolete(tags, Tags.checkObsolete)
    checkObsolete(branches, Branches.checkObsolete)
    Tags.fixNames()
    Branches.fixNames()

    stripMetadata()
  }

  def getSVNRoots(cwd: File = new File(".")) = {
    def getConfig(s: String) = Process("git config --get-all svn-remote.%s.%s".format("svn", s), cwd).lines
    val url = getConfig("url").headOption.getOrElse("")
    def splitRefSpec(s: Stream[String]) = s.map(_.split("\\:")(0).replaceAll("\\*$", "")).toArray
    def getRefSpec(s: String) = splitRefSpec(getConfig(s)).map(safeURLAppend(url, _))
    (getRefSpec("branches"), getRefSpec("tags"))
  }

  def stripMetadata()(implicit options: Clean.Options) = {
    import java.io.File
    import sys.process._

    if (options.stripMetadata) {
      if ((Git.dir /: Seq("info", "grafts"))(new File(_, _)).exists) {
        println("ERROR: Metadata not stripped: grafts exist.")
        true
      } else if (Option((Git.dir /: Seq("refs", "replace"))(new File(_, _)).listFiles).getOrElse(Array()).nonEmpty) {
        println("ERROR: Metadata not stripped: replacement refs exist.")
        true
      } else {
        println("# removing Subversion metadata from Git commit messages")
        if (options.shouldDelete) {
          Seq("git", "filter-branch", "--msg-filter", "sed -e '/^git-svn-id:/d'").!
        }
        false
      }
    } else false
  }
}
