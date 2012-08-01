import scala.Array

object Main extends App {
  val commands = Array(Authors, Clean, Verify, BitbucketPush, MountDiskImage).filter(_.available)

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

  val (helpForCommand, realArgs) = args.partition(_.toLowerCase == "--help")
  val command = realArgs.headOption.flatMap(command => commands.find(_.name == command.toLowerCase)).getOrElse(Help)
  if (helpForCommand.nonEmpty) {
    println(command.name)
    command.usage.foreach(println)
    println()
    println(command.help)
  } else {
    command.parse(args.drop(1)).fold(
      (error) => {
        println(error)
        command.usage.map(u => println(command.name + " usage: [--help] " + u))
      },
      (parsed) => if (command(parsed._1, parsed._2)) sys.exit(1)
    )
  }
}
