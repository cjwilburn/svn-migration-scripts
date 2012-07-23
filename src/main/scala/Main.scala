import scala.Array

object Main extends App {

  val commands = Map(
    "authors" -> authors _,
    "clean-git" -> Main.clean _
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
    val command = args.headOption.map(_.toLowerCase).flatMap(commands.get(_))
                                 .filterNot(_ => urls.isEmpty).getOrElse(help _)

    (command, options, urls)
  }

  /**
   * Show usage information.
   */
  def help(options: Array[String], urls: Array[String]) {
    println("Usage: command [--dry-run] SVN-repository-url")
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println(" - " + c))
  }

  /**
   * Generate the initial author list from the Subversion repository.
   */
  def authors(options: Array[String], arguments: Array[String]) {
    Authors.main(arguments)
  }

  /**
   * Clean the repository after the initial conversion by git-svn.
   */
  def clean(options: Array[String], urls: Array[String]) {
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
