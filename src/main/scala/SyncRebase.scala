package com.atlassian.svn2git

object SyncRebase extends Command {
  val name = "sync-rebase"
  override val usage = None
  val help = "Sync git repository with modified upstream SVN changes"

  override def parse(arguments: Array[String]) = Right(Array(), arguments)

  override def apply(cmd: Cmd, options: Array[String], arguments: Array[String]) = {
    import cmd._
    import git.$

    git("git branch").lines_!.map(_.substring(2)).foreach { branch =>
      val remote = "remotes/" + (if (branch == "master") "trunk" else branch)
      val lstable = $("git", "rev-parse", "--sq", "heads/" + branch)
      val rstable = $("git", "rev-parse", "--sq", remote)
      if (lstable != rstable)
        if (git("git", "rebase", remote, branch).! != 0)
          throw sys.error("error rebasing %s onto %s".format(branch, remote))
    }
    false
  }
}
