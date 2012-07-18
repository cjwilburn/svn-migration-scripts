import AssemblyKeys._

name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.8.8"
)

fork in run := true // We use sys.exit

mainClass in (Compile, run) := Some("Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)

assemblySettings // https://github.com/sbt/sbt-assembly
  
mainClass in assembly <<= mainClass in (Compile, run)

jarName in assembly <<= name(_ + ".jar")
