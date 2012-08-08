package com.atlassian.svn2git

import java.io.File
import java.net.URLDecoder
import sys.process._

object `package` {
  def safeURLAppend(a: String, b: String) = a.stripSuffix("/") + "/" + b.stripPrefix("/")
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
