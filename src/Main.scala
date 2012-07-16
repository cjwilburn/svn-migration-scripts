object Main extends App {
  val commands = Map(
    "annotate-tags" -> AnnotateTags.main _,
    "verify-tags" -> VerifyTags.main _
  )

  def help(x: Array[String]) {
    println("Available commands:")
    commands.keys.toArray.sorted.foreach(c => println("  " + c))
  }

  args.headOption.map(_.toLowerCase).flatMap(commands.get(_)).getOrElse(help _).apply(args.drop(1))
}
