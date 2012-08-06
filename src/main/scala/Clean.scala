package com.atlassian.svn2git

import sys.process._

object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean, stripMetadata: Boolean)

  val name = "clean-git"
  override val usage = Some("[--dry-run] [--no-delete] [--strip-metadata] ...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    Right(arguments.partition(_ startsWith "-"))
  }

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    Git.ensureRootGitDirExists()
    val (branches, tags) = getSVNRoots()
    val dryRun = options.contains("--dry-run")
    implicit val opts = Options(!dryRun, !(dryRun || options.contains("--no-delete")), options.contains("--strip-metadata"))

    Tags.annotate()
    Branches.createLocal()
    Tags.checkObsolete(tags)
    Branches.checkObsolete(branches)
    Tags.fixNames()
    Branches.fixNames()

    stripMetadata()
  }

  def getSVNRoots() = {
    def getConfig(s: String) = "git config svn-remote.%s.%s".format("svn", s).!!.trim
    val url = getConfig("url")
    def splitRefSpec(s: String) = s.split(",").map(_.split("\\:")(0).replace("*", ""))
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
        if (options.shouldDelete) {
          println("# removing Subversion metadata from Git commit messages")
          Seq("git", "filter-branch", "--msg-filter", "sed -e '/^git-svn-id:/d'").!
        }
        false
      }
    } else false
  }
}
