trait Command {
  val name: String
  val usage: Option[String] = None
  val help: String

  def parse(arguments: Array[String]): Either[String, (Array[String], Array[String])]
  def apply(options: Array[String], arguments: Array[String]): Boolean
}
