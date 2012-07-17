import AssemblyKeys._

name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.8.8"
)

mainClass in (Compile, run) := Some("Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)

assemblySettings
  
mainClass in assembly <<= mainClass in (Compile, run)

jarName in assembly <<= name(_ + ".jar")
