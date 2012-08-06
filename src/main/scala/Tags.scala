package com.atlassian.svn2git

import Git._
import sys.process._

object Tags {

  /**
   * Create annotated tags for the Subversion tags converted by git-svn.
   */
  def annotate()(implicit options: Clean.Options) {
    println("# Creating annotated tags...")

    forEachRefFull("refs/remotes/tags/")

      // ignore intermediate references generated by git-svn, like tag@42,
      // when it can not link the source of the copy to an existing remote;
      // otherwise "git-rev-parse --verify" (below) fails because it parses
      // tag@42 as an ordinal specification (i.e. tag@{42})
      .filterNot(isIntermediateRef(_))
      .foreach {

      tag_ref =>
        val tag = tag_ref stripPrefix "refs/remotes/tags/"
        val tree = $("git", "rev-parse", tag_ref)

        // Find the oldest ancestor for which the tree is the same.
        var parent_ref = tag_ref
        while ($("git", "rev-parse", "--quiet", "--verify", parent_ref + "^:") == tree) {
          parent_ref = parent_ref + "^"
        }

        // If this ancestor is in trunk then we can just tag it, otherwise the tag has diverged from trunk
        // and it's actually more like a branch than a tag.
        val target_ref = try {
          val parent = $("git", "rev-parse", parent_ref)
          if ($("git", "merge-base", "refs/remotes/trunk", parent) == parent) {
            parent
          } else {
            println("tag has diverged: " + tag)
            tag_ref
          }
        } catch {
          case ex: RuntimeException => tag_ref
        }

        // Create an annotated tag based on the last commit in the tag
        if (options.shouldCreate) {
          println("Creating annotated tag '%s' at %s.".format(tag, target_ref))
          Seq("git", "show", "-s", "--pretty=format:%s%n%n%b", tag_ref) #|
          Process(Seq("git", "tag", "-f", "-a", "-F", "-", tag, target_ref), None,
              "GIT_COMMITTER_NAME" -> $("git", "show", "-s", "--pretty=format:%an", tag_ref),
              "GIT_COMMITTER_EMAIL" -> $("git", "show", "-s", "--pretty=format:%ae", tag_ref),
              "GIT_COMMITTER_DATE" -> $("git", "show", "-s", "--pretty=format:%ad", tag_ref)) !;
        }
    }
  }

  // Reconcile tags between Git/Subversion.
  def checkObsolete(urls: Array[String])(implicit options: Clean.Options) {
    println("# Checking for obsolete tags...")

    val svnTags = Svn.findItems(urls)
    // Map of (tag as it appears in Subversion -> tag as it appears in Git).
    // e.g. ("my tag" -> "my%20tag")
    val gitTags = forEachRef("refs/tags/").map(tag => (decodeRef(tag), tag)).toMap

    // Remove tags deleted in Subversion.
    val excessTags = gitTags -- svnTags
    if (excessTags.nonEmpty && options.shouldDelete) {
      excessTags.values.foreach { tag =>
        println("Deleting Git tag '%s' not in Subversion.".format(tag))
        Seq("git", "tag", "-d", tag) !
      }
    } else {
      println("No obsolete tags to remove.")
    }

    // Should never do anything if the correct tag roots were given to git-svn.
    (svnTags diff gitTags.keys.toSeq).foreach("WARNING: Subversion tag missing in Git: " + _)
  }

  /**
   * Fix tag names after conversion.
   */
  def fixNames()(implicit options: Clean.Options) {
    println("# Cleaning tag names")

    // list Git tag that needs fixing
    forEachRef("refs/tags/")
      .filter(t => decodeRef(t) != t)
      .foreach { t =>
        val c = t concat "^{commit}" // commit the tag is referring to
        if (options.shouldCreate) {
          println("Replacing tag '%s' with '%s' at %s.".format(t, cleanRef(t), c))
          Seq("git", "show", "-s", "--pretty=format:%s%n%n%b", c) #|
            Process(Seq("git", "tag", "-a", "-F", "-", cleanRef(t), c), None,
                    "GIT_COMMITTER_NAME" -> $("git", "show", "-s", "--pretty=format:%an", c),
                    "GIT_COMMITTER_EMAIL" -> $("git", "show", "-s", "--pretty=format:%ae", c),
                    "GIT_COMMITTER_DATE" -> $("git", "show", "-s", "--pretty=format:%ad", c)) !
        }
        if (options.shouldDelete) Seq("git", "tag", "-d", t) !
    }
  }

}
