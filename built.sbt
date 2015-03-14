name := "maxmind-geoip2-scala"

organization := "com.sanoma.cda"

version := "1.3.4"

scalaVersion := "2.10.5"

crossScalaVersions := Seq("2.10.5", "2.11.6")

libraryDependencies ++= Seq(
  "com.maxmind.geoip2"      % "geoip2"          % "0.9.0",
  "com.twitter"            %% "util-collection" % "6.23.0",
  "org.scalacheck"         %% "scalacheck"      % "1.12.1" % "test",
  "org.scalatest"          %% "scalatest"       % "2.2.3"  % "test"
)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
    case _ =>
      libraryDependencies.value
  }
}
