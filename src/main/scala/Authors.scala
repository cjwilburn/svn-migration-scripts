import dispatch._
import dispatch.liftjson.Js._
import io.Source
import java.lang.String._
import net.liftweb.json._
import scala.Array
import sys.process._
import xml.pull._

object Authors {
  private val executor = new Http with thread.Safety with NoLogging

  // Return a tuple of user name -> email, or None
  private def authorDetails(root: Request, author: String): Option[(String, String)] = {
    try {
      executor(root / "user?username=%s".format(Request.encode_%(author)) ># { json =>
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

  def generateList(args: Array[String]) = fetchList(process(args), convertUsers)
  def generateListForOnDemand(args: Array[String]) = fetchList(processOnDemand(args), convertOnDemandUsers)

  private def fetchList(args: (String, String, String), convertUsers: (String, String, String, Traversable[String]) => Unit) {
    println("# Generating list of authors...")

    val (host, username, password) = args
    val authors = collection.mutable.Set[String]()
    val proc = List(
      "svn", "log",
      "--trust-server-cert",
      "--non-interactive",
      "--no-auth-cache",
      "--xml", "-q",
      host)
      .++ (if (username != null) Seq("--username", username) else Seq())
      .++ (if (password != null) Seq("--password", password) else Seq())
      .run(BasicIO.standard(false) withOutput { is =>
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

    convertUsers(host, username, password, authors)
  }

  private def process(args: Array[String]): (String, String, String) = {
    args match {
      case (Array(host, username, password)) => (host, username, password)
      case (Array(host, username)) => (host, username, readLine("password? "))
      case (Array(host)) => (host, null, null)
      case _ =>
        println("Required: host [username] [password]")
        sys.exit(1)
    }
  }

  private def processOnDemand(args: Array[String]): (String, String, String) = {
    process(args) match {
      case ((host, username, password)) => ("https://" + host + "/svn", username, password)
    }
  }

  private def convertUsers(host: String, username: String, password: String, authors: Traversable[String]) {
    authors.map(format("%1$s = %1$s <%1$s@mycompany.com>", _)).foreach(println)
  }

  private def convertOnDemandUsers(host: String, username: String, password: String, authors: Traversable[String]) {
    val apiRoot = url(host stripSuffix "/svn" concat "/rest/api/latest") as_! (username, password)
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
