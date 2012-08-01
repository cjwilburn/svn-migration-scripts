object MountDiskImage extends Command {
  val name = "mount-disk-image"
  val help = "Mount a case-sensitive disk image at some path."
  override val available = sys.props("os.name") == "Mac OS X"

  def parse(arguments: Array[String]) = Right(Array(), Array())
  def apply(options: Array[String], arguments: Array[String]) = {
    false
  }
}
