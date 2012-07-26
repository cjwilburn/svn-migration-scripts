import scala.Array

object Main extends App {

  val commands = Map(
    "authors" -> authors _,
    "authors-ondemand" -> authorsOnDemand _,
    "clean-git" -> Main.clean _,
    "verify" -> ((options: Array[String], args: Array[String]) => Verify.main(args))
  )

  val (command, options, arguments) = parse(args)
  command(options, arguments)

  /**
   * Parse command line arguments.
   *
   * @param args command line arguments
   * @return desired command, command options and Subversion URLs
   */
  def parse(args: Array[String]) = {
    val (options, arguments) = args.drop(1).partition(_ startsWith "-")

    // If the user doesn't specify a valid command, use “help” as a default one
    val command = args.headOption.map(_.toLowerCase).flatMap(commands.get(_)).getOrElse(help _)

    (command, options, arguments)
  }

  /**
   * Show usage information.
   */
  def help(options: Array[String], urls: Array[String]) {
    println("Unrecognised or missing command")
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println(" - " + c))
  }

  /**
   * Generate the initial author list from the Subversion repository.
   */
  def authors(options: Array[String], arguments: Array[String]) =
    Authors.process(arguments)

  /**
   * Generate the initial author list from an Atlassian OnDemand instance.
   */
  def authorsOnDemand(options: Array[String], arguments: Array[String]) =
    Authors.processOnDemand(arguments)

  /**
   * Clean the repository after the initial conversion by git-svn.
   */
  def clean(options: Array[String], urls: Array[String]) {
    val svnRoots = Set("branches", "tags").map { r => (r, urls.map { u => { u stripSuffix "/"} + '/' + r})}.toMap
    implicit val dryRun = options contains "--dry-run"

    urls match {
      case Array() => println("Required: [--dry-run] <Subversion URL>...")
      case _ =>
        Tags.annotate()
        Branches.createLocal()
        Tags.checkObsolete(svnRoots("tags"))
        Branches.checkObsolete(svnRoots("branches"))
        Tags.fixNames()
        Branches.fixNames()
    }

  }

}
