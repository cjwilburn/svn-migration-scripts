object Main extends App {

  val (options, urls) = args.partition(_ startsWith "-")
  val svnRoots = Set("branches", "tags").map{ r => (r, urls.map{ u => {u stripSuffix "/"} + '/' + r }) }.toMap
  implicit val dryRun = options contains "--dry-run"

  if (urls.isEmpty) {
    println("Usage: SVN-repository-url [--dry-run]")
    println("In the Git repository converted by git-svn.")
  } else {
    Tags.annotate()
    Branches.createLocal()
    Tags.clean(svnRoots("tags"))
    Branches.clean(svnRoots("branches"))
  }

}

