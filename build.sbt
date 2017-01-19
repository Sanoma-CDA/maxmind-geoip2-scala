lazy val packageInfo = Seq(
  organization := "com.sanoma.cda",
  name := "maxmind-geoip2-scala",
  version := "1.5.4"
)

lazy val scalaVersions = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8")
)

val commonBuildLibs = Seq(
  "com.maxmind.geoip2"  % "geoip2"          % "2.3.1",
  "com.twitter"        %% "util-collection" % "6.23.0"
)
val commonTestLibs = Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.2",
  "org.scalatest"  %% "scalatest"  % "2.2.4"
).map(_ % Test)

val scalaLangLibs = Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.3")


lazy val root = (project in file("."))
  .settings(packageInfo: _*)
  .settings(scalaVersions: _*)
  .settings(libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 => commonBuildLibs ++ scalaLangLibs
    case _ => commonBuildLibs
  })
  )
  .settings(libraryDependencies ++= commonTestLibs)
  .settings(scalacOptions ++= Seq("-feature", "-deprecation"))
