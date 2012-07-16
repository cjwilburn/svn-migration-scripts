import AssemblyKeys._

name := "svn-migration-scripts"

version := "0.1"

assemblySettings
  
mainClass in assembly := Some("Main")

jarName in assembly <<= name(_ + ".jar")
