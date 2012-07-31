object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean)

  val name = "clean-git"
  override val usage = Some("[--dry-run] [--no-delete] [--strip-metadata] <url>...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    val (opts, args) = arguments.partition(_ startsWith "-")
    if (args.isEmpty) Left("No Subversion URLs specified") else Right(opts, args)
  }

  def apply(options: Array[String], arguments: Array[String]) = {
    val svnRoots = Set("branches", "tags").map { r => (r, arguments.map { u => { u stripSuffix "/"} + '/' + r})}.toMap
    val dryRun = options.contains("--dry-run")
    implicit val opts = Options(!dryRun, !(dryRun || options.contains("--no-delete")))

    Tags.annotate()
    Branches.createLocal()
    Tags.checkObsolete(svnRoots("tags"))
    Branches.checkObsolete(svnRoots("branches"))
    Tags.fixNames()
    Branches.fixNames()

    false
  }
}
