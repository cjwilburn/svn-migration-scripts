import java.io.{File, IOException}
import sys.process._

object Verify extends Command {
  val name = "verify"
  val help = "Verifies that dependencies are present and the environment is suitable."

  object VersionComparator extends Ordering[String] {
    def atoi(s: String) = {
      var i = 0
      while (i < s.length && Character.isDigit(s(i))) i = i + 1
      try {
        s.substring(0, i).toInt
      } catch {
        case e: NumberFormatException => 0
      }
    }
  
    override def compare(l: String, r: String): Int = {
      val lefts = l.split("[-._]")
      val rights = r.split("[-._]")
      var i = 0
      var v = 0
      while (i < math.min(lefts.length, rights.length) && v == 0) {
        v = atoi(lefts(i)).compareTo(atoi(rights(i)))
        i = i + 1
      }
      if (v != 0) v else lefts.length.compareTo(rights.length)
    }
  }

  def findVersion(s: String*): Either[String, String] =
    (try { (s :+ "--version").lines.headOption }
     catch { case e: IOException => None })
       .flatMap("(?<=version )[^ ]+".r findFirstIn _).toRight("Unable to determine version.")

  def requireVersion(actual: String, required: String): Either[String, String] =
    if (VersionComparator.lt(actual, required)) Left("Version %s required (found %s).".format(required, actual)) else Right(actual)

  case class Dependency(name: String, required: String, invocation: String*)

  def parse(arguments: Array[String]) = Right((Array[String](), Array[String]()))
  
  def apply(options: Array[String], arguments: Array[String]) = {
    var anyError = false
    Array(
      Dependency("Git", "1.7.7.5", "git"),
      Dependency("Subversion", "1.6.17", "svn"),
      Dependency("git-svn", "1.7.7.5", "git", "svn")
    ).map(command => findVersion(command.invocation : _*).right.flatMap(requireVersion(_, command.required)).fold(
      (error) => { anyError = true; println("%s: ERROR: %s".format(command.name, error)) },
      (version) => println("%s: using version %s".format(command.name, version))
    ))

    try {
      val cwd = new File(".")
      val tempFile = File.createTempFile("svn-migration-scripts", ".tmp", cwd)
      tempFile.deleteOnExit()
      if (new File(cwd, tempFile.getName.toUpperCase).exists) {
        println("You appear to be running on a case-insensitive file-system. This is unsupported, and can result in data loss.")
      }
    } catch {
      case e: IOException =>
        anyError = true
        println("Unable to determine whether the file-system is case-insensitive. Case-insensitive file-systems are unsupported, and can result in data loss.")
    }

    anyError
  }
}
