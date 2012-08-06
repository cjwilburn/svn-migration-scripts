package com.atlassian.svn2git

import java.net.URLEncoder
import org.specs2.mutable

class GitTest extends mutable.Specification {

  // tests determined using http://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html
  val testcases = Map (
    "branch test" -> "branch-test",
    "branch.test" -> "branch.test",
    "branch-test." -> "branch-test",
    "branch..test" -> "branch.test",
    "branch...test" -> "branch.test",
    "branch...test." -> "branch.test",
    "branch...test.." -> "branch.test",
    "branch\u0010test" -> "branchtest",
    "branch test\u0010" -> "branch-test",
    "branch\u007Ftest" -> "branchtest", // del character
    "branch/test" -> "branch-test",
    "branch@{test" -> "branch-test",
    "branch~test" -> "branch-test",
    "branch^test" -> "branch-test",
    "branch:test" -> "branch-test",
    "branch?test" -> "branch-test",
    "branch*test" -> "branch-test",
    "branch[test" -> "branch-test"
  )

  "testCleanRef" >> {
    for ((input, expected) <- testcases) {
      expected must equalTo(Git.cleanRef(input))
      expected must equalTo(Git.cleanRef(URLEncoder.encode(input, "UTF-8")))
    }
  }

}
