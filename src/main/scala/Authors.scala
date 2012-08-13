package com.atlassian.svn2git

import java.io.InputStream
import sys.process._
import javax.xml.stream.XMLStreamException

object Authors extends Command {
  val name = "authors"
  override val usage = Some("<hostname> [<username> [<password>]]")
  val help = """Generates an initial authors mapping for the committers to a Subversion
repository.

Each line of the author mapping associates a Subversion commiter with their
full name and e-mail address. For example:

jane.doe = Jane Doe <jane.d@example.org>"""

  def parse(arguments: Array[String]) = {
    val minimumArguments = arguments.headOption.flatMap(onDemandBaseUrl).map(url => 3).getOrElse(1)
    if (arguments.length < minimumArguments || arguments.length > 3)
      Left(if (minimumArguments == 3) "Missing arguments (username and password are required for OnDemand)"
           else "Invalid or missing arguments: re-run with --help for more info")
    else
      Right(Array(), if (arguments.length == 2) arguments :+ readLine("password? ") else arguments)
  }

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]) = {
    val authors = onDemandBaseUrl(arguments.head) match {
      case Some(host) => processOnDemand(host, arguments)
      case None => process(arguments)
    }
    authors.foreach(println)
    false
  }

  def onDemandBaseUrl(url: String) =
    """(?:https?://)?([^/]+\.(?:jira\.com|jira-dev\.com|atlassian\.net))(?:.*?)""".r.findFirstMatchIn(url).map(_.group(1))

  // process/processOnDemand call generateList with two parameters. The first is a ProcessBuilder instance that will
  // call Subversion with the correct arguments to invoke the log command. The second is a function that takes a
  // Subversion username and returns an Option[(full name, e-mail address)].
  private def generateList(svnProcessBuilder: ProcessBuilder) = mapUserDetails(fetchList(svnProcessBuilder)) _

  def mapUserDetails(usernames: Seq[String])(detailsForUser: String => Option[(String, String)]) =
    (usernames, usernames.par.map(detailsForUser).seq).zipped.map(formatUser).sortWith(user_<)

  private def svnCommandLine(url: String, credentials: Option[(String, String)]): ProcessBuilder =
    List("svn", "log", "--trust-server-cert", "--non-interactive", "--no-auth-cache", "--xml", "-q")
      .++(credentials match {
        case Some((user, pass)) => List("--username", user, "--password", pass, url)
        case None => List(url)
      })

  private def process(args: Array[String]) =
    generateList(args match {
      case Array(host, username, password) => svnCommandLine(host, Some((username, password)))
      case Array(host) => svnCommandLine(host, None)
    })(processUsername)

  def processUsername(username: String) =
    if (username.matches(".*@.*\\..*")) {
      // If the username from Subversion is already an email, remove the domain and capitalise each word to
      // generate the full name (ex: john.doe@developer.com -> 'John Doe') and use the username as the generated email
      Some((username.takeWhile(_ != '@').replace('.', ' ').split("\\s").map(capitalise).mkString(" "), username))
    } else {
      Some((username, username + "@mycompany.com"))
    }

  private def processOnDemand(host: String, args: Array[String]) = {
    import dispatch._
    import dispatch.liftjson.Js._
    val Array(url, username, password) = args
    val root = :/(host).secure / "rest" / "api" / "latest" as_! (username, password)
    val executor = new Http with thread.Safety with NoLogging
    generateList(svnCommandLine(url, Some((username, password)))) {
      username =>
        try {
          executor(root / "user" <<? Map("username" -> username) ># parseOnDemandJson)
        } catch {
          case ex: StatusCode => None
        }
    }
  }

  import net.liftweb.json._
  def parseOnDemandJson(json: JValue) = {
    (for {
      JObject(body) <- json
      JField("displayName", JString(name)) <- body
      JField("emailAddress", JString(email)) <- body
    } yield (name.trim, email.trim)).headOption
  }

  private def fetchList(builder: ProcessBuilder) = {
    var authors = Set[String]()
    val proc = builder.run(BasicIO.standard(false) withOutput { in =>
      authors = parseUserXml(in)
    })
    if (proc.exitValue != 0) {
      Console.err.println("SVN command failed!")
      sys.exit(1)
    }

    authors.toSeq
  }

  def parseUserXml(is: InputStream) = {
    import scales.xml._
    import ScalesXml._
    try {
      val iterator = iterate(List("log"l, "logentry"l, "author"l), pullXml(is))
      iterator.map(path => path.children.collect { case Text(t) => t }.foldLeft(new StringBuilder)(_ append _).toString).toSet
    } catch {
      case ex: XMLStreamException =>
        System.err.println("Could not communicate with Subversion: check the URL of the repository, the username and the password are all valid")
        writeExceptionToDisk(ex)
        sys.exit(1)
    }
  }

  private def formatUser(username: String, details: Option[(String, String)]) =
    details.map(details => "%s = %s <%s>".format(username, details._1, details._2)).getOrElse(username)

  private def user_<(l: String, r: String): Boolean = if (l.contains('=')) r.contains('=') && l < r else r.contains('=') || l < r

  private def capitalise(s: String) = if (s.length > 0) s.charAt(0).toUpper + s.drop(1).toLowerCase; else s

}
