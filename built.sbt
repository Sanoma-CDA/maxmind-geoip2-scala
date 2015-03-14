name := "maxmind-geoip2-scala"

organization := "com.sanoma.cda"

version := "1.3.3"

scalaVersion := "2.11.4"

// twitter util doesn't have 2.9.3
crossScalaVersions := Seq("2.9.2", "2.10.4", "2.11.4")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml"       % "1.0.2",
  "com.maxmind.geoip2"      % "geoip2"          % "0.9.0",
  "com.twitter"            %% "util-collection" % "6.23.0",
  "org.scalacheck"         %% "scalacheck"      % "1.12.1" % "test",
  "org.scalatest"          %% "scalatest"       % "2.2.3"  % "test"
)