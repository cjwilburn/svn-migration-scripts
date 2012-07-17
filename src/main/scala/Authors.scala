import dispatch._
import dispatch.liftjson.Js._
import io.Source
import net.liftweb.json._
import sys.process._
import xml.pull._

object Authors {
  def main(args: Array[String]) {
    val host = args(0)
    val username = args(1)
    val password = args(2)

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
      val source = Source.fromInputStream(is)
      val reader = new XMLEventReader(source)
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
    require(proc.exitValue == 0)

    val h = new Http with thread.Safety {
      override def make_logger = new Logger {
        def info(message: String, items: Any*) {}
        def warn(message:String, items: Any*) {}
      }
    }
    val groupedAuthors = authors.par.map { author =>
      val u = url("https://%s/rest/api/latest/user?username=%s".format(host, author))
      h(u.as_!(username, password).># { json =>
        for {
          JObject(body) <- json
          JField("displayName", JString(displayName)) <- body
          JField("emailAddress", JString(emailAddress)) <- body
        } yield "%s = %s <%s>".format(author, displayName.trim, emailAddress.trim)
      }).headOption.getOrElse(author)
    }.seq.groupBy(_ contains '=')

    groupedAuthors.getOrElse(false, Set[String]()).toArray.sorted.foreach(println)
    groupedAuthors.getOrElse(true, Set[String]()).toArray.sorted.foreach(println)
  }
}
