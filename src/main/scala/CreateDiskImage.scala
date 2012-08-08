package com.atlassian.svn2git

import java.io.File
import scala.sys.process._

object CreateDiskImage extends Command {
  val name = "create-disk-image"
  override val usage = Some("<size-in-gb> <image-name>")
  val help = "Mount a case-sensitive disk image at some path."
  override val available = sys.props("os.name") == "Mac OS X"
  val homeDir = sys.props("user.home")

  def parse(arguments: Array[String]) =
    arguments match {
      case Array(size, imageName) =>
        try {
          val parsedSize = size.toInt
          if (parsedSize < 0)
            Left("Can not create a disk image with negative size.")
          else if (parsedSize == 0)
            Left("Can not create a disk image with zero size.")
          else if (parsedSize > 100)
            Left("Can not create a disk image larger than 100GB.")
          else
            Right(Array(), arguments)
        } catch {
          case ex: NumberFormatException =>
            Left("Invalid size argument '%s'.".format(size))
        }
      case _ => Left("Invalid arguments")
    }

  def apply(cmd: Cmd, options: Array[String], arguments: Array[String]) = {
    val size = arguments(0) + "g"
    val imageName = arguments(1)
    val imagePath = /(homeDir, imageName + ".sparseimage") // Append suffix to stop wrong image type being created
    val mountPath = /(homeDir, imageName)
    run((Seq("hdiutil", "create", "-size", size, imagePath, "-type", "SPARSE", "-fs", "HFS+", "-fsargs", "-s", "-volname", imageName) #&&
         Seq("hdiutil", "attach", imagePath, "-mountpoint", mountPath)),
        "The disk image was created successfully and mounted as: " + mountPath,
        "The disk image could not be created. Check if an image already exists at " + imagePath)
  }

  def /(d: String, f: String) = new File(d, f).getAbsolutePath

  def run(p: ProcessBuilder, success : String, failure : String) = {
    val r = p.! == 0
    println(if (r) success; else failure)
    r
  }

}
