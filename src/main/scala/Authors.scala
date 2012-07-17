import dispatch._
import dispatch.liftjson.Js._
import io.Source
import net.liftweb.json._
import sys.process._
import xml.pull._

object Authors {
  val executor = new Http with thread.Safety {
    override def make_logger = new Logger {
      def info(message: String, items: Any*) {}
      def warn(message:String, items: Any*) {}
    }
  }

  def main(args: Array[String]) {
    if (args.length != 3) {
      println("args: host username password")
      sys.exit(1)
    }

    val Array(host, username, password) = args
    val authors = collection.mutable.Set[String]()
    val proc = Process(Array(
      "svn", "log",
      "--trust-server-cert",
      "--non-interactive",
      "--username", username,
      "--password", password,
      "--no-auth-cache",
      "--xml", "-q",
      "https://%s/svn".format(host)
    )).run(BasicIO.standard(false) withOutput { is =>
      val reader = new XMLEventReader(Source.fromInputStream(is))
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

    val groupedAuthors = authors.par.map { author =>
      val authorUrl = url("https://%s/rest/api/latest/user?username=%s".format(host, author))
      executor(authorUrl.as_!(username, password).># { json =>
        for {
          JObject(body) <- json
          JField("displayName", JString(displayName)) <- body
          JField("emailAddress", JString(emailAddress)) <- body
        } yield "%s = %s <%s>".format(author, displayName.trim, emailAddress.trim)
      }).headOption.getOrElse(author)
    }.seq.partition((author) => !(author contains '='))
    groupedAuthors._1.toArray.sorted.foreach(println)
    groupedAuthors._2.toArray.sorted.foreach(println)
  }
}
