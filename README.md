Introduction to maxmind-geoip2-scala
====================================

This is a simple Scala wrapper for the MaxMind GeoIP2-java library: http://maxmind.github.io/GeoIP2-java/
Note that the GeoIP2 is still in beta.

This project is based on the https://github.com/snowplow/scala-maxmind-geoip from Snowplow!

Installation
============

I suggest that you clone this repository and publish to local repository to be used in another project.

sbt +publish-local

After that, you can use it in your sbt by adding the following dependency:

libraryDependencies += "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.0"

You should also be able to generate a fat jar with Assembly.
We chose not to include the data file into the jar as you should update that form time to time.

Usage
=====

Here is a simple usage example:

import com.sanoma.cda.geoip.MaxMindIpGeo
val geoIp = MaxMindIpGeo("/data/MaxMind/GeoLite2-City.mmdb", 1000)
println(geoIp.getLocation("123.123.123.123"))

Data
====
Download (and unzip) data from here:
http://dev.maxmind.com/geoip/geoip2/geolite2/
http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
