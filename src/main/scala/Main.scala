import scala.Array

object Main extends App {
  val commands = Array(Authors, Clean, Verify)

  object Help extends Command {
    val name = "help"
    val help = "Help"
    def parse(arguments: Array[String]) = Right(Array(), Array())
    def apply(opts: Array[String], arguments: Array[String]) = {
      println("Unrecognised or missing command")
      println("Available commands:")
      commands.sortBy(_.name).map("- " + _.name).foreach(println)

      true
    }
  }

  val command = args.headOption.flatMap(command => commands.find(_.name == command.toLowerCase)).getOrElse(Help)
  command.parse(args.drop(1)).fold(
    (error) => {
      println(error)
      command.usage.map(u => println(command.name + " usage: " + u))
    },
    (parsed) => sys.exit(if (command(parsed._1, parsed._2)) 1 else 0)
  )
}
