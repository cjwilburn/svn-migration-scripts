package com.atlassian.svn2git

import java.net.URLEncoder
import org.scalatest.FunSuite

class GitTest extends FunSuite {

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

  test("testCleanRef") {
    for ((input, expected) <- testcases) {
      assert(expected === Git.cleanRef(input), "Input: " + input)
      assert(expected === Git.cleanRef(URLEncoder.encode(input, "UTF-8")), "Input: " + input)
    }
  }

}
