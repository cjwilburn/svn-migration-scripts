object Clean extends Command {
  case class Options(shouldCreate: Boolean, shouldDelete: Boolean, stripMetadata: Boolean)

  val name = "clean-git"
  override val usage = Some("[--dry-run] [--no-delete] [--strip-metadata] <url> ...")
  val help = "Cleans conversion artefacts from a converted Subversion repository."

  def parse(arguments: Array[String]) = {
    val (opts, args) = arguments.partition(_ startsWith "-")
    if (args.isEmpty) Left("No Subversion URLs specified") else Right(opts, args)
  }

  def apply(options: Array[String], arguments: Array[String]): Boolean = {
    val svnRoots = Set("branches", "tags").map { r => (r, arguments.map { u => { u stripSuffix "/"} + '/' + r})}.toMap
    val dryRun = options.contains("--dry-run")
    implicit val opts = Options(!dryRun, !(dryRun || options.contains("--no-delete")), options.contains("--strip-metadata"))

    Tags.annotate()
    Branches.createLocal()
    Tags.checkObsolete(svnRoots("tags"))
    Branches.checkObsolete(svnRoots("branches"))
    Tags.fixNames()
    Branches.fixNames()

    stripMetadata()
  }

  def stripMetadata()(implicit options: Clean.Options) = {
    import java.io.File
    import sys.process._

    if (options.stripMetadata) {
      if ((Git.dir /: Seq("info", "grafts"))(new File(_, _)).exists) {
        println("ERROR: Metadata not stripped: grafts exist.")
        true
      } else if (Option((Git.dir /: Seq("refs", "replace"))(new File(_, _)).listFiles).getOrElse(Array()).nonEmpty) {
        println("ERROR: Metadata not stripped: replacement refs exist.")
        true
      } else {
        if (options.shouldDelete) {
          println("# removing Subversion metadata from Git commit messages")
          Seq("git", "filter-branch", "--msg-filter", "sed -e '/^git-svn-id:/d'").!
        }
        false
      }
    } else false
  }
}
