import sys.process._

object Tags {

  /**
   * Create annotated tags for the Subversion tags converted by git-svn.
   */
  def annotate()(implicit dryRun: Boolean) {
    Seq("git", "for-each-ref", "--format=%(refname)", "refs/remotes/tags/*").lines.foreach {
      tag_ref =>
        val tag = tag_ref stripPrefix "refs/remotes/tags/"
        val tree = $("git", "rev-parse", tag_ref)

        // Find the oldest ancestor for which the tree is the same.
        var parent_ref = tag_ref
        while ($("git", "rev-parse", "--quiet", "--verify", parent_ref + "^:") == tree) {
          parent_ref = parent_ref + "^"
        }

        // If this ancestor is in trunk then we can just tag it, otherwise the tag has diverged from trunk and it's actually more like a branch than a tag.
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

        // Create an annotated tag based on the last commit in the tag, and delete the "branchy" ref for the tag.
        println("Creating annotated tag: " + tag_ref)
        if (!dryRun) {
          Seq("git", "show", "-s", "--pretty=format:%s%n%n%b", tag_ref) #|
          Process(Seq("git", "tag", "-a", "-F", "-", tag, target_ref), None,
              "GIT_COMMITTER_NAME" -> $("git", "show", "-s", "--pretty=format:%an", tag_ref),
              "GIT_COMMITTER_EMAIL" -> $("git", "show", "-s", "--pretty=format:%ae", tag_ref),
              "GIT_COMMITTER_DATE" -> $("git", "show", "-s", "--pretty=format:%ad", tag_ref)) !;
          Seq("git", "update-ref", "-d", tag_ref).!
        }
    }
  }

  /**
   * Remove tags that does not exist in Subversion.
   */
  def clean(urls: Array[String])(implicit dryRun: Boolean) {
    val svnItems = Svn.findSvnItems(urls)
    val gitItems = "git for-each-ref refs/tags --format='%(refname:short)'".lines_!
    (gitItems diff svnItems).foreach {
      item =>
        println("Deleting Git tag not in Subversion: " + item)
        if (!dryRun) ("git tag -d " + item).!
    }
    (svnItems diff gitItems).foreach {
      item =>
        println("This Subversion tag is not in Git: " + item)
    }
  }

  private def $(s: String*): String = Process(s).!!.stripLineEnd

}
