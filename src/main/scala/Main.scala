object Main extends App {

  def wrap(command: Array[String] => Unit) = (options: Array[String], args: Array[String]) => command(args)

  val commands = Map(
    "authors" -> wrap(Authors.main _),
    "clean-git" -> Main.clean _,
    "sync-rebase" -> wrap(SyncRebase.main _)
  )

  val (command, options, urls) = parse(args)
  command(options, urls)

  /**
   * Parse command line arguments.
   *
   * @param args command line arguments
   * @return desired command, command options and Subversion URLs
   */
  def parse(args: Array[String]) = {
    val (options, urls) = args.drop(1).partition(_ startsWith "-")

    // If the user doesn't specify a valid command, use “help” as a default one
    val command = args.headOption.map(_.toLowerCase).flatMap(commands.get(_)).getOrElse(help _)

    (command, options, urls)
  }

  /**
   * Show usage information.
   */
  def help(options: Array[String], urls: Array[String]) {
    println("Usage: command [--dry-run] args...")
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println(" - " + c))
  }

  /**
   * Clean the repository after the initial conversion by git-svn.
   */
  def clean(options: Array[String], urls: Array[String]) {
    if (urls isEmpty) {
      println("Usage: clean-git [--dry-run] svn-url...")
    } else {
      val svnRoots = Set("branches", "tags").map { r => (r, urls.map { u => { u stripSuffix "/"} + '/' + r})}.toMap
      implicit val dryRun = options contains "--dry-run"

      Tags.annotate()
      Branches.createLocal()
      Tags.checkObsolete(svnRoots("tags"))
      Branches.checkObsolete(svnRoots("branches"))
      Tags.fixNames()
      Branches.fixNames()
    }
  }

}
