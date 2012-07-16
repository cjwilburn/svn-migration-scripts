#!/usr/bin/env scala
::!#
import java.io.File
import sys.process._

def findSvnItems(svnUrls: Array[String]): Array[String] = {
  val strippedUrls = svnUrls.map(_ stripSuffix "/")
  val allBranchUrls = strippedUrls.flatMap { url =>
    ("svn ls " + url).lines_!.map(url + "/" + _.stripSuffix("/"))
  }
  (allBranchUrls diff strippedUrls).map(new File(_).getName)
}

val (options, urls) = args.partition(_ startsWith "-")
val dryRun = options contains "--dry-run"

val svnItems = findSvnItems(urls)
val gitItems = "git for-each-ref refs/tags --format='%(refname:short)'".lines_!
(gitItems diff svnItems).foreach { item =>
  println("git tag not in svn: " + item)
  if (!dryRun) ("git tag -d " + item).!
}
(svnItems diff gitItems).foreach { item =>
  println("svn tag not in git: " + item)
}
