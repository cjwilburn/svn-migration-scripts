package com.atlassian.svn2git

import org.specs2.mutable
import org.apache.commons.io.FileUtils
import java.io.File

class SyncRebaseTest extends mutable.Specification {

  "test SyncRebase rebases branch" >> {
    Verify.withTempGitDir {
      dir =>
        val cmd = new Cmd(dir)
        import cmd.git
        git("git", "init").!
        git("git", "commit", "--allow-empty", "-m", "Initial commit").!
        git("git", "commit", "--allow-empty", "-m", "Second commit").!
        git("git", "update-ref", "refs/remotes/trunk", "master").!
        git("git", "reset", "HEAD^").!
        FileUtils.write(new File(dir, "test.txt"), "a")
        git("git", "add", "test.txt").!
        git("git", "commit", "-a", "-m", "Third commit").!
        val trunk = git.$("git", "rev-parse", "trunk")
        SyncRebase(cmd, Array(), Array())
        git.$("git", "rev-parse", "master^") must equalTo(trunk)
    }
  }
}
