package com.atlassian.svn2git

import java.io._
import java.net.URLDecoder
import sys.process._
import org.apache.commons.io.{FileUtils, IOUtils}
import scala.Some
import scala.Console

object `package` {
  def safeURLAppend(a: String, b: String) = a.stripSuffix("/") + "/" + b.stripPrefix("/")

  def returning[T](t: T)(f: T => Unit) = {
    f(t); t
  }

  def writeExceptionToDisk(t: Throwable) {
    val f = File.createTempFile("error-script-", ".txt")
    val s = new PrintWriter(new BufferedWriter(new FileWriter(f)))
    try {
      t.printStackTrace(s)
      println("Error written to " + f.getAbsolutePath)
    } catch {
      case e: IOException => System.err.println("Could not write the error to a temp file. Here is the complete stacktrace:")
                             e.printStackTrace(System.err)
                             f.deleteOnExit()
    } finally {
      IOUtils.closeQuietly(s)
    }
  }
}

class Svn {

  def findItems(svnUrls: Array[String]): Array[String] = {
    val strippedUrls = svnUrls.map(_ stripSuffix "/")
    val allBranchUrls = strippedUrls.flatMap {
      url =>
        Seq("svn", "ls", url).lines_!.map(url + "/" + _.stripSuffix("/"))
    }
    (allBranchUrls diff strippedUrls).map(new File(_).getName)
  }

}

class Git(cwd: File) {
  def isIntermediateRef(ref: String) = {
    "^.+@\\d+$".r.findAllIn(ref).hasNext
  }

  def decodeRef(ref: String) = {
    URLDecoder.decode(ref, "UTF-8")
  }

  def cleanRef(ref: String) = {
    // reference: http://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html
    decodeRef(ref).filterNot(Character.isISOControl _)
      .trim()
      .stripSuffix("?")
      .stripSuffix(".lock")
      .replaceAll("\\.+$", "")
      .replaceAll("\\.{2,}", ".")
      .replaceAll("""(?:\s+|\@\{|[~^:*?/]+|\[+|\]+)""", "-")
  }

  def dir: File = new File(sys.env.getOrElse("GIT_DIR", ".git"))

  def forEachRefFull(pattern: String) = this("git", "for-each-ref", pattern, "--format=%(refname)").lines

  def forEachRef(pattern: String) = forEachRefFull(pattern).map(_ stripPrefix pattern)

  def gc() = this("git", "gc", "--prune=now").lines_!

  def getRootGitDir() = {
    try {
      Some(new File($("git", "rev-parse", "--show-toplevel")))
    } catch {
      case _ => None
    }
  }

  def ensureRootGitDirExists() = {
    if (getRootGitDir().isEmpty) {
      System.err.println("Command must be run from within a Git repository")
      sys.exit(1)
    }
  }

  def warnIfLargeRepository(f: Unit => Unit = _ => ()) = {
    val size = FileUtils.sizeOfDirectory(dir)
    if (size > (1 << 30)) {
      println()
      println("### Warning: your repository is larger than 1GB (" + size / (1 << 20) + " Mb)")
      println("### See https://dvcsroute.atlassian.net/wiki/x/XQAQ on how to reduce the size of your repository.")
      println()
      f()
      false
    } else true
  }

  def $(s: String*): String = this(s: _*).!!.stripLineEnd

  def apply(cmd: String) = Process(cmd, cwd)

  def apply(cmd: String*) = Process(cmd, cwd)

  def apply(cmd: Seq[String], extraEnv: (String, String)*) = Process(cmd, cwd, extraEnv: _*)

}

class Cmd(cwd: File = new File(".")) {
  val git: Git = new Git(cwd)
  val svn: Svn = new Svn
  def println(s: Any) = Console.println(s)
}
