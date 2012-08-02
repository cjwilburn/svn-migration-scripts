import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._
import sys.process._

object BitbucketPush extends Command {
  val name = "bitbucket-push"
  val help = "Push to a repository on Bitbucket, optionally creating it."
  override val usage = Some("<username> <password> [<owner>] <repository-name>")

  def parse(args: Array[String]) = {
    val (options, arguments) = args.partition(_ == "--ssh")
    arguments match {
      case Array(user, pass, owner, name) => Right(options, arguments)
      case Array(user, pass, name) => Right(options, Array(user, pass, user, name))
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

  def existing(http: Http, api: Request, name: String, owner: String): Option[String] =
    http(api / "user" / "repositories" ># { json =>
      for {
        JArray(body) <- json
        JObject(repo) <- body
        JField("owner", JString(repoOwner)) <- repo
        JField("name", JString(repoName)) <- repo
        JField("slug", JString(slug)) <- repo if repoOwner == owner && repoName == name
      } yield slug
    }).headOption

  def repoSlug(api: Request, name: String, owner: String): Either[String, String] = {
    val http = new Http with NoLogging
    existing(http, api, name, owner).toRight("non-extant").left.flatMap(error => create(http, api, name, owner))
  }

  def ensureRemote(remoteUrl: String): Either[String, String] =
    Either.cond("git remote show bitbucket" #|| Seq("git", "remote", "add", "bitbucket", remoteUrl) ! ProcessLogger(s => ()) == 0,
      "bitbucket", "Error creating Git remote: " + remoteUrl)

  def push(remote: String): Either[String, String] =
    Either.cond((Seq("git", "push", "--all", remote) #&& Seq("git", "push", "--tags", remote)).! == 0,
      "Successfully pushed to Bitbucket", "Pushing repository to Bitbucket failed.")

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    import Request.{encode_% => e}
    val Array(username, password, owner, name) = arguments

    (for {
      slug <- repoSlug(:/("api.bitbucket.org").secure.as_!(username, password) / "1.0", name, owner).right
      remote <- ensureRemote("https://%s:%s@bitbucket.org/%s/%s".format(e(username), e(password), owner, slug)).right
      result <- push(remote).right
    } yield result).fold(
      error => { println("ERROR: " + error); true },
      message => { println(message); false }
    )
  }
}
