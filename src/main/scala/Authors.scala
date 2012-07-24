import collection._
import dispatch._
import dispatch.liftjson.Js._
import io.Source
import java.lang.String._
import net.liftweb.json._
import sys.process._
import util.Sorting._
import xml.pull._

object Authors {
  private val executor = new Http with thread.Safety with NoLogging

  // Return a tuple of user name -> email, or None
  private def authorDetails(root: Request, author: String): Option[(String, String)] = {
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

  def generateList(args: Array[String]) = printUsers(convertUsers(fetchList(process(args))))
  def generateListForOnDemand(args: Array[String]) = printUsers(convertOnDemandUsers(fetchList(processOnDemand(args))))

  private def fetchList(args: (String, Option[String], Option[String])) = {
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
      .++ (username.map(Seq("--username", _)).getOrElse(Seq()))
      .++ (password.map(Seq("--password", _)).getOrElse(Seq()))
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

    (host, username, password, authors)
  }

  private def process(args: Array[String]) = {
    args match {
      case Array(host, username, password) => (host, Some(username), Some(password))
      case Array(host, username) => (host, Some(username), Some(readLine("password? ")))
      case Array(host) => (host, None, None)
      case _ =>
        println("Required: host [username [password]]")
        sys.exit(1)
    }
  }

  private def processOnDemand(args: Array[String]) = {
    process(args) match {
      case (_, None, _) =>
        println("Credentials are required to connect to your OnDemand instance")
        sys.exit(1)
      case (host, username, password) => ("https://" + host + "/svn", username, password)
    }
  }

  def detailRecord(username: String, fullname: String, email: String) = "%s = %s <%s>".format(username, fullname, email)

  private def convertUsers(args: (String, Option[String], Option[String], Traversable[String])): Traversable[String] = {
    val (host, username, password, authors) = args
    authors.map(author => detailRecord(author, author, author + "@mycompany.com"))
  }

  // Convert authors to "svnauthor = Git Author <email@address>" where possible.
  private def convertOnDemandUsers(args: (String, Option[String], Option[String], GenTraversable[String])): Traversable[String] = {
    val (host, username, password, authors) = args
    val apiRoot = url(host stripSuffix "/svn" concat "/rest/api/latest") as_! (username.get, password.get)
    authors
      .par
      .map(author => authorDetails(apiRoot, author).map(details => detailRecord(author, details._1, details._2)).getOrElse(author))
      .seq
  }

  def printUsers(users: Traversable[String]) {
    // Print out the users we couldn't map before the users we could. This is to make it easier for the customer to identify which authors need identification.
    val grouped = users.partition(_ contains '=')
    (stableSort(grouped._2.toSeq) ++ stableSort(grouped._1.toSeq)).foreach(println)
  }
}
