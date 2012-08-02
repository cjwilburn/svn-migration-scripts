import sbt._
import ProguardPlugin._

object SvnMigrationScripts extends Build {

  lazy val proguard = proguardSettings ++ Seq(
    proguardOptions := Seq(keepMain("com.atlassian.svn2git.Main"),
      "-keepattributes *Annotation*",
      "-keep public class com.atlassian.** { public protected *; }",
      "-keep class * implements org.xml.sax.EntityResolver",
      "-keepclassmembers class * { ** MODULE$; }",
      "-keepnames public class com.atlassian.** ",
      "-keepdirectories",
      "-dontnote"
    )
  )

  lazy val root = Project("root", file(".")).settings(proguard: _*).dependsOn(dispatchLiftJson)
  lazy val dispatchLiftJson = uri("git://github.com/dispatch/dispatch-lift-json#0.1.1")
}
