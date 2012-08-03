object MountDiskImage extends Command {
  val name = "mount-disk-image"
  override val usage = Some("<size-in-gb> <disk-image-file> <mount-point>")
  val help = "Mount a case-sensitive disk image at some path."
  override val available = sys.props("os.name") == "Mac OS X"

  def parse(arguments: Array[String]) =
    arguments match {
      case Array(size, diskImageFile, mountPoint) =>
        try {
          val parsedSize = size.toInt
          if (parsedSize < 0)
            Left("Cowardly refusing to create a disk image with negative size.")
          else if (parsedSize == 0)
            Left("Cowardly refusing to create a disk image with zero size.")
          else if (parsedSize > 100)
            Left("Cowardly refusing to create a disk image larger than 100GB.")
          else
            Right(Array(), arguments)
        } catch {
          case ex: NumberFormatException =>
            Left("Invalid size argument '%s'.".format(size))
        }
      case _ => Left("Invalid arguments")
    }

  def apply(options: Array[String], arguments: Array[String]) = {
    import sys.process._
    val size = arguments(0) + "g"
    val imagePath = arguments(1) + ".sparseimage" // Append suffix to stop wrong image type being created
    val mountPath = arguments(2)
    (Seq("hdiutil", "create", "-size", size, imagePath, "-type", "SPARSE", "-fs", "HFS+", "-fsargs", "-s", "-volname", "svn-git-migration") #&&
      Seq("hdiutil", "attach", imagePath, "-mountpoint", mountPath)).! != 0
  }
}
