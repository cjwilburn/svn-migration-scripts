import sys.process._

object Authors extends Command {
  val name = "authors"
  override val usage = Some("hostname [username [password]]")
  val help = "Generates an initial authors mapping for the committers to a Subversion repository."

  def parse(arguments: Array[String]) = {
    val minimumArguments = arguments.headOption.flatMap(onDemandBaseUrl).map(url => 3).getOrElse(1)
    if (arguments.length < minimumArguments || arguments.length > 3)
      Left(if (minimumArguments == 3) "Invalid arguments (username and password required for OnDemand)" else "Invalid arguments")
    else
      Right(Array(), if (arguments.length == 2) arguments :+ readLine("password? ") else arguments)
  }

  def apply(options: Array[String], arguments: Array[String]) = {
    onDemandBaseUrl(arguments.head) match {
      case Some(host) => processOnDemand(host +: arguments.tail)
      case None => process(arguments)
    }
    false
  }

  private def onDemandBaseUrl(url: String) =
    """(?:https?://)?([^/]+\.(?:jira\.com|jira-dev\.com|atlassian\.net))(?:.*?)""".r.findFirstMatchIn(url).map(_.group(1))

  // process/processOnDemand call generateList with two parameters. The first is a ProcessBuilder instance that will
  // call Subversion with the correct arguments to invoke the log command. The second is a function that takes a
  // Subversion username and returns an Option[(full name, e-mail address)].
  private def generateList(svnProcessBuilder: ProcessBuilder, detailsForUser: String => Option[(String, String)]) {
    val usernames = fetchList(svnProcessBuilder)
    (usernames, usernames.par.map(detailsForUser).seq).zipped.map(formatUser).sortWith(user_<).foreach(println)
  }

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
    }, (username: String) => Some((username, username + "@mycompany.com")))

  private def processOnDemand(args: Array[String]) = {
    import dispatch._
    import dispatch.liftjson.Js._
    import net.liftweb.json._
    val Array(host, username, password) = args
    val root = :/(host).secure / "rest" / "api" / "latest" as_! (username, password)
    val executor = new Http with thread.Safety with NoLogging
    generateList(svnCommandLine("https://" + host + "/svn", Some((username, password))),
      (username: String) => try {
        executor(root / "user" <<? Map("username" -> username) ># { json =>
          (for {
            JObject(body) <- json
            JField("displayName", JString(name)) <- body
            JField("emailAddress", JString(email)) <- body
          } yield (name.trim, email.trim)).headOption
        })
      } catch {
        case ex: StatusCode => None
      })
  }

  private def fetchList(builder: ProcessBuilder) = {
    println("# Generating list of authors...")

    val authors = collection.mutable.Set[String]()
    val proc = builder.run(BasicIO.standard(false) withOutput {
      is => {
        import scales.xml._
        import ScalesXml._
        val iterator = iterate(List("log"l, "logentry"l, "author"l), pullXml(is))
        iterator.map(path => path.children.collect { case Text(t) => t }.foldLeft(new StringBuilder)(_ append _).toString).foreach(authors += _)
      }
    })

    if (proc.exitValue != 0) {
      println("SVN command failed!")
      sys.exit(1)
    }

    authors.toSeq
  }

  private def formatUser(username: String, details: Option[(String, String)]) =
    details.map(details => "%s = %s <%s>".format(username, details._1, details._2)).getOrElse(username)

  private def user_<(l: String, r: String): Boolean = if (l.contains('=')) r.contains('=') && l < r else r.contains('=') || l < r
}
