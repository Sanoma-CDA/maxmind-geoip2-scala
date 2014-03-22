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

libraryDependencies += "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.2"

You should also be able to generate a fat jar with Assembly.
We chose not to include the data file into the jar as you should update that form time to time.

Data
====
Download (and unzip) data from here:
http://dev.maxmind.com/geoip/geoip2/geolite2/
http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz

Usage
=====

Here is a simple usage example:

import com.sanoma.cda.geoip.MaxMindIpGeo
val geoIp = MaxMindIpGeo("/data/MaxMind/GeoLite2-City.mmdb", 1000)
println(geoIp.getLocation("123.123.123.123"))


Geo-package
===========

Version 1.2 introduces geo package that contains some geo primitives as well as some algorithms. This is the first stab at the APIs to see if they are usefull, not completely thought out yet - comments are wellcome.
The main motivation of these classes were to be able to do geo fencing to see if given point (latitude, longitude) from the MaxMind library falls inside some pre-defined area.
Unfortunately, this slightly changed the API of the IpLocation class. Namely the tuple that previously held latitude and longitude was changed in Point. There are implicit conversions available between Tuple2 and Point though.

The classes of the Geo package are simple. The design started out as having no direct relation to geo coordinates and worked with any coordinate system. The main use cases that we have include relatively small areas that are far from the data boundary or the poles. However, there are 2 distance functions calculating the distance between 2 points on earth. These were introduced for the circle class - which is defined by having a radius in meters around center point that is expected to be in degrees.

The GeoAreaMap is designed to hold the different geo areas, such as the circles, rectangles and simple polygons. It can give the ID of the are that the given point belongs to. Note that it will always search the areas in the same order - so remember to give the most probable areas in the beginning of the list. The data structure will not optimize this by itself.

Here is an example of doing lookup using GeoAreaMap

    import com.sanoma.cda.geo._
    val turku = Point(60.45, 22.25)
    val helsinki = Point(60.17, 24.94)
    val tamminiemi = Point(60.1892,24.8838)
    val mantyniemi = Point(60.1844,24.8968)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    val tCircle = Circle(tamminiemi, 1000)
    val hRectangle = Rectangle(lowerLeft = (60.15, 24.84), topRight = (60.20, 25.00))
    val aPoly = Polygon(List((60.30, 24.88), (60.34, 24.95), (60.295, 25.02)))

    val data = List("tamminiemi" -> tCircle, "helsinki" -> hCircle, "airport" -> aPoly, "hRect" -> hRectangle)
    val gmap = GeoAreaMap.fromSeq(data)

    gmap.get(turku) // None
    gmap.get(mantyniemi) // Some("tamminiemi")
    gmap.getAll(mantyniemi) // List(tamminiemi, helsinki, hRect)

