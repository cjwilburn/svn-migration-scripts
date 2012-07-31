import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._

object BitbucketPush extends Command {
  val name = "bitbucket-push"
  val help = "Push to a repository on Bitbucket, optionally creating it."
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

  def push(username: String, password: String, owner: String, slug: String): Either[String, Unit] = {
    import Request.{encode_% => e}
    import sys.process._
    val remote = "https://%s:%s@bitbucket.org/%s/%s".format(e(username), e(password), owner, slug)
    val process =
      ("git remote show bitbucket" #|| Seq("git", "remote", "add", "bitbucket", remote)) #&&
      ("git push --all bitbucket" #&& "git push --tags bitbucket")
    if (process.! == 0) Right(()) else Left("Pushing the repository to Bitbucket failed.")
  }

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    val Array(username, password, owner, name) = arguments

    val http = new Http with NoLogging
    val api = :/("api.bitbucket.org").secure.as_!(username, password) / "1.0"

    (for {
      _ <- existing(http, api, name, owner).left
      slug <- create(http, api, name, owner).right
      result <- push(username, password, owner, slug).right
    } yield result).fold(
      error => { println("An error occurred while pushing to Bitbucket: " + error); true },
      unit => { println("Successfully pushed to Bitbucket."); false }
    )
  }
}
