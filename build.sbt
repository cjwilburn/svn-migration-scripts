import AssemblyKeys._

name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "net.databinder" %% "dispatch-http" % "0.8.8",
  "org.scalesxml" %% "scales-xml" % "0.3.1"
)

fork in run := true // We use sys.exit

mainClass in (Compile, run) := Some("Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)

assemblySettings // https://github.com/sbt/sbt-assembly
  
mainClass in assembly <<= mainClass in (Compile, run)

jarName in assembly <<= name(_ + ".jar")
