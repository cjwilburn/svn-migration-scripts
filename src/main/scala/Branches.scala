package com.atlassian.svn2git

object Branches {

  /**
   * Create local branches out of svn branches.
   */
  def createLocal(cmd: Cmd)(implicit options: Clean.Options) {
    import cmd._
    println("# Creating local branches...")

    git.forEachRefFull("refs/remotes/")
      .filterNot(_ startsWith "refs/remotes/tags")
      .foreach {
        branch_ref =>
          // delete the "refs/remotes/" prefix
          val branch = branch_ref stripPrefix "refs/remotes/"

          // create a local branch ref only if it's not trunk (which is already mapped to master)
          // and if it is not an intermediate branch (ex: foo@42)
          if (branch != "trunk" && !git.isIntermediateRef(branch)) {
            println("Creating the local branch '%s' for Subversion branch '%s'.".format(branch, branch_ref))
            if (options.shouldCreate) {
              git("git", "branch", "-f", "-t", branch, branch_ref) !
            }
          }
      }
  }

  // Reconcile branches between Git/Subversion.
  def checkObsolete(cmd: Cmd)(urls: Array[String])(implicit options: Clean.Options) {
    import cmd._
    println("# Checking for obsolete branches...")

    val svnBranches = svn.findItems(urls)
    // Map of (branch as it appears in Subversion -> branch as it appears in Git).
    // e.g. ("my branch" -> "my%20branch")
    val gitBranches = git.forEachRef("refs/heads/").filterNot(_ == "master").map(branch => git.decodeRef(branch) -> branch).toMap

    // Remove branches deleted in Subversion.
    val excessBranches = gitBranches -- svnBranches
    if (excessBranches.nonEmpty) {
      excessBranches.values.foreach { branch =>
        println("Deleting Git branch '%s' not in Subversion.".format(branch))
        if (options.shouldDelete) {
          git("git", "branch", "-D", branch) !
        }
      }
    } else {
      println("No obsolete branches to remove.")
    }

    // Should never do anything if the correct branch roots were given to git-svn.
    (svnBranches diff gitBranches.keys.toSeq).foreach("WARNING: Subversion branch missing in Git: " + _)
  }

  /**
   * Fix branch names after conversion.
   */
  def fixNames(cmd: Cmd)(implicit options: Clean.Options) {
    import cmd._
    println("# Cleaning branch names")

    // list Git branches that needs fixing
    git.forEachRef("refs/heads/")
      .filter(r => git.decodeRef(r) != r)
      .foreach { r =>
        println("Replacing branch '%s' with '%s'.".format(r, git.cleanRef(r)))
        if (options.shouldDelete) {
          git("git", "branch", "-m", r, git.cleanRef(r)) !
        }
      }
  }

}
