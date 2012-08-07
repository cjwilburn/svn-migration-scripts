package com.atlassian.svn2git

import Git._
import sys.process._

object Branches {

  /**
   * Create local branches out of svn branches.
   */
  def createLocal()(implicit options: Clean.Options) {
    println("# Creating local branches...")

    forEachRefFull("refs/remotes/")
      .filterNot(_ startsWith "refs/remotes/tags")
      .foreach {
        branch_ref =>
          // delete the "refs/remotes/" prefix
          val branch = branch_ref stripPrefix "refs/remotes/"

          // create a local branch ref only if it's not trunk (which is already mapped to master)
          // and if it is not an intermediate branch (ex: foo@42)
          if (branch != "trunk" && !isIntermediateRef(branch)) {
            println("Creating the local branch '%s' for Subversion branch '%s'.".format(branch, branch_ref))
            if (options.shouldCreate) {
              Seq("git", "branch", "-f", "-t", branch, branch_ref) !
            }
          }
      }
  }

  // Reconcile branches between Git/Subversion.
  def checkObsolete(urls: Array[String])(implicit options: Clean.Options) {
    println("# Checking for obsolete branches...")

    val svnBranches = Svn.findItems(urls)
    // Map of (branch as it appears in Subversion -> branch as it appears in Git).
    // e.g. ("my branch" -> "my%20branch")
    val gitBranches = forEachRef("refs/heads/").filterNot(_ == "master").map(branch => decodeRef(branch) -> branch).toMap

    // Remove branches deleted in Subversion.
    val excessBranches = gitBranches -- svnBranches
    if (excessBranches.nonEmpty) {
      excessBranches.values.foreach { branch =>
        println("Deleting Git branch '%s' not in Subversion.".format(branch))
        if (options.shouldDelete) {
          Seq("git", "branch", "-D", branch) !
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
  def fixNames()(implicit options: Clean.Options) {
    println("# Cleaning branch names")

    // list Git branches that needs fixing
    forEachRef("refs/heads/")
      .filter(r => decodeRef(r) != r)
      .foreach { r =>
        println("Replacing branch '%s' with '%s'.".format(r, cleanRef(r)))
        if (options.shouldDelete) {
          Seq("git", "branch", "-m", r, cleanRef(r)) !
        }
      }
  }

}
