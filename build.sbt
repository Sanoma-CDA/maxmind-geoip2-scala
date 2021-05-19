organization := "com.sanoma.cda"
name := "maxmind-geoip2-scala"
version := "1.5.5"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "com.maxmind.geoip2"      % "geoip2"          % "2.15.0",
  "org.scala-lang.modules" %% "scala-xml"       % "1.3.0",
  "com.twitter"            %% "util-collection" % "19.1.0",
  "org.scalacheck"         %% "scalacheck"      % "1.15.2" % Test,
  "org.scalatest"          %% "scalatest"       % "3.2.9"  % Test
)
scalacOptions ++= Seq("-feature", "-deprecation")
