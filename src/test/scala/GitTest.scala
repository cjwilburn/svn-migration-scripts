import java.net.URLEncoder
import org.junit.Test
import org.junit.Assert._

class GitTest {

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

  @Test
  def testCleanRef() {
    for ((input, expected) <- testcases) {
      assertEquals("Input: " + input, expected, Git.cleanRef(input))
      assertEquals("Input: " + input, expected, Git.cleanRef(URLEncoder.encode(input, "UTF-8")))
    }
  }

}
