lazy val packageInfo = Seq(
  organization := "com.sanoma.cda",
  name := "maxmind-geoip2-scala",
  version := "1.5.4"
)

lazy val scalaVersions = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1")
)

val commonBuildLibs = Seq(
  "com.maxmind.geoip2"     % "geoip2"           % "2.8.1",
  "com.twitter"            %% "util-collection" % "6.42.0",
  "org.scala-lang.modules" %% "scala-xml"       % "1.0.6"
)
val scala210BuildLibs = Seq(
  "com.maxmind.geoip2"     % "geoip2"           % "2.8.1",
  "com.twitter"            %% "util-collection" % "6.34.0"
)
val commonTestLibs = Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "org.scalatest"  %% "scalatest"  % "3.0.1"
).map(_ % Test)

lazy val root = (project in file("."))
  .settings(packageInfo: _*)
  .settings(scalaVersions: _*)
  .settings(libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => scala210BuildLibs
    case _ => commonBuildLibs
  })
  )
  .settings(libraryDependencies ++= commonTestLibs)
  .settings(scalacOptions ++= Seq("-feature", "-deprecation"))
