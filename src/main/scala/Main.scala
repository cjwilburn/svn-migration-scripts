import sys.process._

object `package` {
  def $(s: String*): String = s.!!.stripLineEnd
}

object Main extends App {
  val commands = Map(
    "annotate-tags" -> AnnotateTags.main _,
    "authors" -> Authors.main _,
    "verify-tags" -> VerifyTags.main _
  )

  def help(x: Array[String]) {
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println("  " + c))
  }

  // If the user doesn't specify a valid command, use “help” as a default command.
  val c = args.headOption.map(_.toLowerCase).flatMap(commands.get(_)).getOrElse(help _)
  c(args.drop(1))
}
