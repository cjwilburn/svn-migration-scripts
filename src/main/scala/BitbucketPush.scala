package com.atlassian.svn2git

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._
import sys.process.ProcessLogger

object BitbucketPush extends Command {
  val name = "bitbucket-push"
  val help = "Push to a repository on Bitbucket, optionally creating it."
  override val usage = Some("<username> <password> [<owner>] <repository-name>")

  def parse(args: Array[String]) = {
    val (options, arguments) = args.partition(_ == "--ssh")
    arguments match {
      case Array(user, pass, owner, name) => Right(options, arguments)
      case Array(user, pass, name) => Right(options, Array(user, pass, user, name))
      case _ => Left("Invalid or missing arguments: re-run with --help for more info")
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
    } >! {
      case ex: StatusCode =>
        System.err.println("Could not verify if the repository exists on Bitbucket: check the username or the password are valid")
        writeExceptionToDisk(ex)
        sys.exit(1)
    }).headOption

  def repoSlug(api: Request, name: String, owner: String): Either[String, String] = {
    val http = new Http with NoLogging
    existing(http, api, name, owner).toRight("non-extant").left.flatMap(error => create(http, api, name, owner))
  }

  def ensureRemote(git: Git, remoteName: String, remoteUrl: String): Either[String, String] =
    Either.cond(git("git", "remote", "show", remoteName) #|| git("git", "remote", "add", remoteName, remoteUrl) ! ProcessLogger(s => ()) == 0,
      remoteName, "Error creating Git remote: " + remoteUrl)

  def push(git: Git, remote: String): Either[String, String] =
    Either.cond((git("git", "push", "--all", remote) #&& git("git", "push", "--tags", remote)).! == 0,
      "Successfully pushed to Bitbucket", "Pushing repository to Bitbucket failed.")

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]): Boolean = {
    import Request.{ encode_% => e }
    import cmd.git
    val Array(username, password, owner, name) = arguments

    // gc the repository and ask for an explicit confirmation if it is very large
    git.gc()
    git.warnIfLargeRepository({
      _ =>
        do {
          println("Are you sure you want to continue? (Y/N)")
        } while (readLine().toLowerCase match {
          case "y" | "yes" => false
          case "n" | "no"  => sys.exit()
          case _ => true
        })
    })

    (for {
      slug <- repoSlug(:/("api.bitbucket.org").secure.as_!(username, password) / "1.0", name, owner).right
      remote <- {
        if (options.contains("--ssh"))
          ensureRemote(git, "bitbucket-ssh", "git@bitbucket.org:%s/%s".format(owner, slug))
        else
          ensureRemote(git, "bitbucket", "https://%s:%s@bitbucket.org/%s/%s".format(e(username), e(password), owner, slug))
      }.right
      result <- push(git, remote).right
    } yield result).fold(
      error => { println("ERROR: " + error); true },
      message => { println(message); false }
    )
  }
}
