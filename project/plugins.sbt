resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.3")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0")
