trait Command {
  val name: String
  val usage: Option[String] = None
  val help: String
  val available: Boolean = true

  def parse(arguments: Array[String]): Either[String, (Array[String], Array[String])]
  def apply(options: Array[String], arguments: Array[String]): Boolean
}
