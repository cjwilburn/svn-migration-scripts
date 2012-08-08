package com.atlassian.svn2git

import org.specs2.mutable
import org.apache.commons.io.FileUtils
import java.io.File

class CleanTest extends mutable.Specification {

  def writeSVNRemoteConfig(dir: File, extra: String = "") = {
    FileUtils.writeStringToFile(new File(dir, ".git/config"),
      """
        |[svn-remote "svn"]
        |	url = https://a/b
        |	fetch = trunk:refs/remotes/trunk
        |	branches = branches/*:refs/remotes/*
        |	tags = tags/*:refs/remotes/tags/*
      """.stripMargin + "\n" + extra)
  }

  "test getSVNRoot" >> {
    Verify.withTempGitDir {
      dir =>
        writeSVNRemoteConfig(dir, "branches = branches2/*/3:refs/remotes/*")
        val (branches, tags) = Clean.getSVNRoots(new Cmd(dir))
        branches must equalTo(Array("https://a/b/branches/", "https://a/b/branches2/*/3"))
        tags must equalTo(Array("https://a/b/tags/"))
    }
  }
}
