package com.atlassian.svn2git

import sys.process._

object SyncRebase extends Command {
  val name = "sync-rebase"
  override val usage = None
  val help = "Sync git repository with modified upstream SVN changes"

  override def parse(arguments: Array[String]) = Right(Array(), arguments)

  override def apply(options: Array[String], arguments: Array[String]) = {
    "git branch".lines_!.map(_.substring(2)).foreach { branch =>
      val remote = "remotes/" + (if (branch == "master") "trunk" else branch)
      val lstable = $("git", "rev-parse", "--sq", "heads/" + branch)
      val rstable = $("git", "rev-parse", "--sq", remote)
      if (lstable != rstable)
        if (List("git", "rebase", remote, branch).! != 0)
          throw sys.error("error rebasing %s onto %s".format(branch, remote))
    }
    false
  }
}
