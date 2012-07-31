import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._

object BitbucketCreate extends Command {
  val name = "bitbucket-create"
  val help = "Create a repository on Bitbucket."
  override val usage = Some("<username> <password> [<owner>] <repository-name>")

  def parse(arguments: Array[String]) = {
    arguments match {
      case Array(user, pass, owner, name) => Right(Array(), arguments)
      case Array(user, pass, name) => Right(Array(), Array(user, pass, user, name))
      case _ => Left("Invalid arguments")
    }
  }

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    val Array(username, password, owner, name) = arguments

    val http = new Http with NoLogging
    val api = :/("api.bitbucket.org").secure.as_!(username, password) / "1.0"

    http((api / "repositories" << Array(
      "name" -> name,
      "scm" -> "git",
      "owner" -> owner,
      "is_private" -> "True"
    )).>|.>! {
      case ex: StatusCode =>
        println("Error " + ex.code + " creating repository:")
        println(ex.contents)
        return true
    })

    false
  }
}
