import Git._
import sys.process._

object Branches {

  /**
   * Create local branches out of svn branches.
   */
  def createLocal()(implicit dryRun: Boolean) {
    Seq("git", "for-each-ref", "--format=%(refname)", "refs/remotes/").lines.foreach {
      branch_ref =>
        // delete the "refs/remotes/" prefix
        val branch = branch_ref stripPrefix "refs/remotes/"

        // create a local branch ref only if it's not trunk (which is already mapped to master)
        // and if it is not an intermediate branch (ex: foo@42)
        if (!"trunk".equals(branch) && !isIntermediateRef(branch)) {
          println("Creating the local branch " + branch)
          if (!dryRun) {
            Seq("git", "branch", "-f", "-t", branch, branch_ref).!
          }
        }
    }
  }

  /**
   * Remove branches that does not exist in Subversion.
   */
  def clean(svnUrls: Array[String])(implicit dryRun: Boolean) {
    // find the list of branches in SVN
    val svnItems = Svn.findSvnItems(svnUrls)

    // find the list of branches in Git
    // (split is safe as ref names cannot contain spaces)
    val gitItems = Seq("git", "for-each-ref", "refs/heads", "--format=%(refname)").lines
      .map(_ stripPrefix "refs/heads/")
      // do not compare master
      .filterNot(_ equals "master")
      // clean the Git branch names
      .map(cleanRef _)

    // remove the branches deleted in SVN
    // (git-svn does not index branch and tag deletions, and keep their remotes)
    gitItems.foreach {
      branch =>
        if (!(svnItems contains branch)) {
          println("Deleting the Git branch: " + branch)
          if (!dryRun) {
            Seq("git", "branch", "-D", branch).!
          }
        }
    }

    svnItems.foreach {
      branch =>
        if (!(gitItems contains branch)) {
          println("This Subversion branch is not in Git: " + branch)
        }
    }
  }

}
