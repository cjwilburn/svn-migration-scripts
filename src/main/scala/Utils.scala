import java.io.File
import java.net.URLDecoder
import sys.process._

object `package` {
  def $(s: String*): String = s.!!.stripLineEnd
}

object Svn {

  def findItems(svnUrls: Array[String]): Array[String] = {
    val strippedUrls = svnUrls.map(_ stripSuffix "/")
    val allBranchUrls = strippedUrls.flatMap {
      url =>
        Seq("svn", "ls", url).lines_!.map(url + "/" + _.stripSuffix("/"))
    }
    (allBranchUrls diff strippedUrls).map(new File(_).getName)
  }

}

object Git {
  def isIntermediateRef(ref: String) = {
    "^.+@\\d+$".r.findAllIn(ref).hasNext
  }

  def decodeRef(ref: String) = {
    URLDecoder.decode(ref, "UTF-8")
  }

  def cleanRef(ref: String) = {
    // reference: http://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html
    decodeRef(ref).filterNot(Character.isISOControl _)
                  .trim()
                  .stripSuffix("?")
                  .stripSuffix(".lock")
                  .replaceAll("\\.+$", "")
                  .replaceAll("\\.{2,}", ".")
                  .replaceAll("""(?:\s+|\@\{|[~^:*?/]+|\[+|\]+)""", "-")
  }

  def dir: File = new File(sys.env.getOrElse("GIT_DIR", ".git"))
}
