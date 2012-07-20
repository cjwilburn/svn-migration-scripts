import Git._
import sys.process._

object Branches {

  /**
   * Create local branches out of svn branches.
   */
  def createLocal()(implicit dryRun: Boolean) {
    println("# Creating local branches...")

    Seq("git", "for-each-ref", "--format=%(refname)", "refs/remotes/").lines.foreach {
      branch_ref =>
        // delete the "refs/remotes/" prefix
        val branch = branch_ref stripPrefix "refs/remotes/"

        // create a local branch ref only if it's not trunk (which is already mapped to master)
        // and if it is not an intermediate branch (ex: foo@42)
        if (!("trunk" == branch) && !isIntermediateRef(branch)) {
          if (dryRun) {
            println("Creating the local branch " + branch)
          } else {
            Seq("git", "branch", "-f", "-t", branch, branch_ref).!
          }
        }
    }
  }

  /**
   * Remove branches that does not exist in Subversion.
   */
  def checkObsolete(svnUrls: Array[String])(implicit dryRun: Boolean) {
    println("# Checking for obsolete branches...")

    // find the list of branches in SVN
    val svnBranches = Svn.findSvnItems(svnUrls)

    // find the list of branches in Git
    // (split is safe as ref names cannot contain spaces)
    val gitBranches = Seq("git", "for-each-ref", "refs/heads", "--format=%(refname)").lines
      .map(_ stripPrefix "refs/heads/")
      // do not compare master
      .filterNot(_ == "master")
      // clean the Git branch names
      .map(ref => (cleanRef(ref), ref)) // (cleaned-up name, actual ref name),
                                        // ex: ("my branch", "my%20branch")
                                        // the cleaned-up name will match the actual name of the branch in Subversion

    // remove the branches deleted in SVN
    // (git-svn does not index branch and tag deletions, and keep their remotes)
    gitBranches.foreach {
      case (branch, ref) =>
        if (!(svnBranches contains branch)) {
          if (dryRun) {
            println("Deleting the Git branch: " + ref)
          } else {
            Seq("git", "branch", "-D", ref).!
          }
        }
    }

    // check for missing branches in Git
    // (note: this should never happen if the correct branch root(s) were given to git-svn)
    svnBranches.foreach {
      branch =>
        if (gitBranches.filter{ case (b, _) => b == branch }.isEmpty) {
          println("This Subversion branch is not in Git: " + branch)
        }
    }
  }

}
