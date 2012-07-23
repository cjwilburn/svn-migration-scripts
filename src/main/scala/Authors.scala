import dispatch._
import dispatch.liftjson.Js._
import io.Source
import net.liftweb.json._
import sys.process._
import xml.pull._

object Authors {
  val executor = new Http with thread.Safety with NoLogging

  // Return a tuple of user name -> email, or None
  def authorDetails(root: Request, author: String): Option[(String, String)] = {
    try {
      executor(root / "user" <<? Map("username" -> author) ># { json =>
        (for {
          JObject(body) <- json
          JField("displayName", JString(name)) <- body
          JField("emailAddress", JString(email)) <- body
        } yield (name.trim, email.trim)).headOption
      })
    } catch {
      case ex: StatusCode => None
    }
  }

  def main(args: Array[String]) {
    if (args.length != 3) {
      println("args: host username password")
      sys.exit(1)
    }

    val Array(host, username, password) = args
    val authors = collection.mutable.Set[String]()
    val proc = List(
      "svn", "log",
      "--trust-server-cert",
      "--non-interactive",
      "--username", username,
      "--password", password,
      "--no-auth-cache",
      "--xml", "-q",
      "https://" + host + "/svn"
    ).run(BasicIO.standard(false) withOutput { is =>
      val reader = new XMLEventReader(Source.fromInputStream(is))
      // Add the content of the author elements to the authors set declared above.
      reader.foreach(_ match {
        case EvElemStart(null, "author", _, _) => {
          authors += reader.takeWhile(_ match {
            case EvElemEnd(null, "author") => false
            case _ => true
          }).collect(_ match {
            case EvText(content) => content
          }).foldLeft(new StringBuilder)(_ append _).toString
        }
        case _ => ()
      })
    })
    if (proc.exitValue != 0) {
      println("SVN command failed!")
      sys.exit(1)
    }

    val apiRoot = url("https://" + host + "/rest/api/latest") as_! (username, password)
    val groupedAuthors = authors
      .par
      // Convert authors to "svnauthor = Git Author <email@address>" where possible.
      .map(author => authorDetails(apiRoot, author).map(details => "%s = %s <%s>".format(author, details._1, details._2)).getOrElse(author))
      .seq
      .partition(author => !(author contains '='))
    // Print out the users we couldn't map before the users we could. This is to make it easier for the customer to identify which authors need identification.
    groupedAuthors._1.toArray.sorted.foreach(println)
    groupedAuthors._2.toArray.sorted.foreach(println)
  }
}
