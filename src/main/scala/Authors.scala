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

  def generateList(args: Array[String]) {
    val (host, username, password) = process(args)
    val authors = fetchList(host, username, password)
    val convertedAuthors = authors.map(author => userDetails(host, username, password)(author).map(details => detailRecord(author, details._1, details._2)).getOrElse(author))
    printUsers(convertedAuthors)
  }

  def generateListForOnDemand(args: Array[String]) {
    val (host, username, password) = processOnDemand(args)
    val authors = fetchList(host, username, password)
    val convertedAuthors = authors.map(author => onDemandUserDetails(host, username, password)(author).map(details => detailRecord(author, details._1, details._2)).getOrElse(author))
    printUsers(convertedAuthors)
  }

  private def fetchList(host: String, username: Option[String], password: Option[String]) = {
    println("# Generating list of authors...")

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

    authors
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

  // Convert authors to "svnauthor = Git Author <email@address>" where possible.
  def userDetails(host: String, username: Option[String], password: Option[String]) = (author: String) => Some((author, author + "@mycompany.com"))
  def onDemandUserDetails(host: String, username: Option[String], password: Option[String]) = {
    val root = url(host stripSuffix "/svn" concat "/rest/api/latest") as_! (username.get, password.get)
    (author: String) => try {
      executor(root / "user" <<? Map("username" -> author) ># { json =>
        (for {
          JObject(body) <- json
          JField("displayName", JString(name)) <- body
          JField("emailAddress", JString(email)) <- body
        } yield (name.trim, email.trim)).headOption
      }): Option[(String, String)]
    } catch {
      case ex: StatusCode => None
    }
  }
  
  def printUsers(users: Traversable[String]) {
    // Print out the users we couldn't map before the users we could. This is to make it easier for the customer to identify which authors need identification.
    val grouped = users.partition(_ contains '=')
    (stableSort(grouped._2.toSeq) ++ stableSort(grouped._1.toSeq)).foreach(println)
  }
}
