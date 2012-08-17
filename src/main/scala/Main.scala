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

import scala.Array

object Main extends App {
  val commands = Array(Authors, Clean, Verify, BitbucketPush, SyncRebase, CreateDiskImage).filter(_.available)

  object Help extends Command {
    val name = "help"
    val help = "Help"
    def parse(arguments: Array[String]) = Right(Array(), Array())
    def apply(cmd: Cmd, opts: Array[String], arguments: Array[String]) = {
      import cmd.println
      println("Unrecognised or missing command")
      println("Available commands:")
      commands.sortBy(_.name).map("- " + _.name).foreach(println)

      true
    }
  }

  val (helpForCommand, realArgs) = args.partition(_.toLowerCase == "--help")
  val command = realArgs.headOption.flatMap(command => commands.find(_.name == command.toLowerCase)).getOrElse(Help)
  if (helpForCommand.nonEmpty) {
    println(command.name)
    command.usage.foreach(println)
    println()
    println(command.help)
  } else {
    command.parse(args.drop(1)).fold(
      (error) => {
        println(error)
        command.usage.map(u => println(command.name + " usage: [--help] " + u))
      },
      (parsed) => if (command(new Cmd(), parsed._1, parsed._2)) sys.exit(1)
    )
  }
}
