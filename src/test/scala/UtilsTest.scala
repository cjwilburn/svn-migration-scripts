package com.atlassian.svn2git

import org.specs2._
import java.io.File

class UtilsTest extends mutable.Specification {

  val dir = new File(".")

  "getRootGitDir" >> {

    "same directory" >> {
      Git.getRootGitDir(dir) must equalTo(Some(dir))
    }
    "up one directory" >> {
      Git.getRootGitDir(new File(dir, "src")) must equalTo(Some(dir))
    }
    "not found" >> {
      Git.getRootGitDir(new File("/tmp")) must equalTo(None)
    }
  }

}
