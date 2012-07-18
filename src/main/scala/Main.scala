object Main extends App {
  val commands = Map(
    "annotate-tags" -> AnnotateTags.main _,
    "verify-tags" -> VerifyTags.main _
  )

  def help(x: Any) {
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println("  " + c))
  }

  // If the user doesn't specify a valid command, use “help” as a default command.
  val command = args.headOption.map(_.toLowerCase).flatMap(commands.get _).getOrElse(help _)
  command(args.drop(1))
}
