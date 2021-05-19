organization := "com.sanoma.cda"
name := "maxmind-geoip2-scala"
version := "1.5.5"

scalaVersion := "2.11.12"

// @formatter:off
libraryDependencies ++= Seq(
  "com.maxmind.geoip2"      % "geoip2"          % "2.15.0",
  "org.scala-lang.modules" %% "scala-xml"       % "1.3.0",
  "com.twitter"            %% "util-collection" % "19.1.0",
  "org.scalacheck"         %% "scalacheck"      % "1.15.2" % Test,
  "org.scalatest"          %% "scalatest"       % "3.2.9"  % Test
)
// @formatter:on
scalacOptions ++= Seq("-feature", "-deprecation")

/**
 * uncomment in order to publish manually
 * */
githubOwner := "seekingalpha"
githubRepository := "maxmind-geoip2-scala"

/**
 * to override a package uncomment these lines
 * publishConfiguration := publishConfiguration.value.withOverwrite(true)
 * publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
 * */