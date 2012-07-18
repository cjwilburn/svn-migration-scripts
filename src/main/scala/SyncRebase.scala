import sys.process._

object SyncRebase {
  def main(args: Array[String]) {
    "git branch".lines_!.map(_.substring(2)).foreach { branch =>
      val remote = "remotes/" + (if (branch == "master") "trunk" else branch)
      val lstable = ("git rev-parse --sq heads/" + branch).!!
      val rstable = ("git rev-parse --sq " + remote).!!
      if (lstable != rstable) {
        if ("git rebase %s %s".format(remote, branch).! != 0)
          throw sys.error("error rebasing %s onto %s".format(branch, remote))
      }
    }
  }
}
