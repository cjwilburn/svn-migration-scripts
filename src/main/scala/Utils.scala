import java.io.File
import java.net.URLDecoder
import sys.process._

object Svn {

  def findSvnItems(svnUrls: Array[String]): Array[String] = {
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

  def cleanRef(ref: String) = {
    URLDecoder.decode(ref, "UTF-8")
  }

}
