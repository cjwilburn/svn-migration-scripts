name := "svn-migration-scripts"

version := "0.1"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.7" % "test",
  "org.specs2" %% "specs2" % "1.9" % "test",
  "net.databinder" %% "dispatch-http" % "0.8.8",
  "org.scalesxml" %% "scales-xml" % "0.3.1"
)

fork in run := true // We use sys.exit

mainClass in (Compile, run) := Some("com.atlassian.svn2git.Main")

mainClass in (Compile, packageBin) <<= mainClass in (Compile, run)

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

makeInJarFilter <<= (makeInJarFilter) {
  (makeInJarFilter) => {
    (file) => file match {
      case "httpcore-4.1.4.jar" => makeInJarFilter(file) + ",!META-INF/NOTICE*,!META-INF/LICENSE*"
      case "httpclient-4.1.3.jar" => makeInJarFilter(file) + ",!META-INF/NOTICE*,!META-INF/LICENSE*"
      case _ => makeInJarFilter(file)
    }
  }
}
