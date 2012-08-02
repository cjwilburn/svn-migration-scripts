name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test",
  "net.databinder" %% "dispatch-http" % "0.8.8",
  "org.scalesxml" %% "scales-xml" % "0.3.1"
)

fork in run := true // We use sys.exit

mainClass in (Compile, run) := Some("com.atlassian.svn2git.Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)
