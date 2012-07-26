import java.io.{File, IOException}

object Verify {
  object VersionComparator extends Ordering[String] {
    def atoi(s: String) = {
      var i = 0
      while (i < s.length && Character.isDigit(s(i))) i = i + 1
      try {
        s.substring(0, i).toInt
      } catch {
        case e: NumberFormatException => 0
      }
    }
  
    override def compare(l: String, r: String): Int = {
      val lefts = l.split("[-.]")
      val rights = r.split("[-.]")
      var i = 0
      var v = 0
      while (i < math.min(lefts.length, rights.length) && v == 0) {
        v = atoi(lefts(i)).compareTo(atoi(rights(i)))
        i = i + 1
      }
      if (v != 0) v else lefts.length.compareTo(rights.length)
    }
  }
  
  def main(args: Array[String]) {
    val requiredGitVersion = "1.7.7.5"
    Array(
      {
        try {
          val gitVersion = $("git", "--version")
          if (VersionComparator.lt(gitVersion, requiredGitVersion)) {
            Some("Git version %s required, but only found %s".format(requiredGitVersion, gitVersion))
          } else None
        } catch {
          case e: IOException =>
            Some("Could not find Git version %s or greater.".format(requiredGitVersion))
        }
      },
      {
        val requiredSubversionVersion = "1.6.17"
        try {
          val subversionVersion = $("svn", "--version")
          if (VersionComparator.lt(subversionVersion, requiredSubversionVersion)) {
            Some("Subversion version %s required, but only found %s.".format(requiredSubversionVersion, subversionVersion))
          } else None
        } catch {
          case e: IOException =>
            Some("Could not find Subversion version %s or greater.".format(requiredSubversionVersion))
        }
      },
      {
        try {
          val gitSvnVersion = $("git", "svn", "--version")
          if (VersionComparator.lt(gitSvnVersion, requiredGitVersion)) {
            Some("git-svn version %s required, but only found %s.".format(requiredGitVersion, gitSvnVersion))
          } else None
        } catch {
          case e: IOException =>
            Some("Could not find git-svn version %s or greater.".format(requiredGitVersion))
        }
      },
      {
        try {
          val tempFile = File.createTempFile("svn-migration-scripts", ".tmp", new File("."))
          tempFile.deleteOnExit()
          if (new File(new File("."), tempFile.getName.toUpperCase).exists) {
            Some("You appear to be running on a case-insensitive file-system. This is unsupported, and can result in data loss.")
          } else None
        } catch {
          case e: IOException =>
            Some("Unable to determine whether the file-system is case-insensitive. Case-insensitive file-systems are unsupported, and can result in data loss.")
        }
      }
    ).flatten.foreach(println)
  }
}
