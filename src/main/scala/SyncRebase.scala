/**
 * Copyright 2012 Atlassian
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.atlassian.svn2git

object SyncRebase extends Command {
  val name = "sync-rebase"
  override val usage = None
  val help = "Sync git repository with modified upstream SVN changes"

  override def parse(arguments: Array[String]) = Right(Array(), arguments)

  override def apply(cmd: Cmd, options: Array[String], arguments: Array[String]) = {
    import cmd._
    import git.$

    git("git branch").lines_!.map(_.substring(2)).foreach { branch =>
      val remote = "remotes/" + (if (branch == "master") "trunk" else branch)
      val lstable = $("git", "rev-parse", "--sq", "heads/" + branch)
      val rstable = $("git", "rev-parse", "--sq", remote)
      if (lstable != rstable)
        if (git("git", "rebase", remote, branch).! != 0)
          throw sys.error("error rebasing %s onto %s".format(branch, remote))
    }
    false
  }
}
