package com.atlassian.svn2git

import org.specs2.mutable
import org.apache.commons.io.FileUtils
import java.io.File

class CleanTest extends mutable.Specification {

  "test getSVNRoot" >> {
    Verify.withTempGitDir {
      dir =>
        FileUtils.writeStringToFile(new File(dir, ".git/config"),
          """
            |[svn-remote "svn"]
            |	url = https://a/b
            |	fetch = trunk:refs/remotes/trunk
            |	branches = branches/*:refs/remotes/*
            |	branches = branches2/*/3:refs/remotes/*
            |	tags = tags/*:refs/remotes/tags/*
          """.stripMargin)
        val (branches, tags) = Clean.getSVNRoots(dir)
        branches must equalTo(Array("https://a/b/branches/", "https://a/b/branches2/*/3"))
        tags must equalTo(Array("https://a/b/tags/"))
    }
  }
}
