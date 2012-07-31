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

  def create(http: Http, api: Request, name: String, owner: String): Either[String, String] =
    http(api / "repositories" << Array(
      "name" -> name,
      "scm" -> "git",
      "owner" -> owner,
      "is_private" -> "True"
    ) ># { json =>
      for {
        JObject(body) <- json
        JField("slug", JString(slug)) <- body
      } yield slug
    } >! {
      case ex: StatusCode => return Left(ex.contents)
    }).headOption.toRight("Creation was successful but response lacked slug.")

  def existing(http: Http, api: Request, name: String, owner: String): Either[String, String] =
    http(api / "user" / "repositories" ># { json =>
      for {
        JArray(body) <- json
        JObject(repo) <- body
        JField("owner", JString(repoOwner)) <- repo
        JField("name", JString(repoName)) <- repo
        JField("slug", JString(slug)) <- repo if repoOwner == owner && repoName == name
      } yield slug
    }).headOption.toRight("Repository does not exist.")

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    val Array(username, password, owner, name) = arguments

    val http = new Http/* with NoLogging*/
    val api = :/("api.bitbucket.org").secure.as_!(username, password) / "1.0"

    var slug = for {
      _ <- existing(http, api, name, owner).left
      s <- create(http, api, name, owner).right
    } yield s

    slug.fold(
      error => true,
      slug => false
    )
  }
}
