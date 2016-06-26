/*
 * Copyright (c) 2014 Sanoma Oyj. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.sanoma.cda.geo

abstract class GeoArea {
  /**
   * Tells whether this are contains the given point.
   */
  def contains(p: Point): Boolean
  /**
   * This should be faster method for quick determination wheter we should even look into this.
   * Like does the bounding box of the area include the point.
   */
  def mayContain(p: Point): Boolean

  /**
   * This returns the extreme points - used to create global bounding box
   */
  def getExtremes: List[Point]

}


/**
 * Rectable
 */
class Rectangle(maxLatitude: Double, maxLongitude: Double, minLatitude: Double, minLongitude: Double) extends GeoArea {
  /** Low (minLongitude) and left (minLatitude) sides are included */
  override def contains(p: Point) = p.latitude >= minLatitude && p.latitude < maxLatitude && p.longitude >= minLongitude && p.longitude < maxLongitude
  /** all sides are included as this is to search for possible inclusions */
  override def mayContain(p: Point) = p.latitude >= minLatitude && p.latitude <= maxLatitude && p.longitude >= minLongitude && p.longitude <= maxLongitude

  override def getExtremes = List(Point(maxLatitude, maxLongitude), Point(minLatitude, minLongitude))

  def getCorners = List(
    Point(minLatitude, minLongitude),
    Point(minLatitude, maxLongitude),
    Point(maxLatitude, maxLongitude),
    Point(maxLatitude, minLongitude)
  )
}

object Rectangle {
  def apply(lowerLeft: Point, upperRight: Point) = new Rectangle(
    math.max(lowerLeft.latitude, upperRight.latitude),
    math.max(lowerLeft.longitude, upperRight.longitude),
    math.min(lowerLeft.latitude, upperRight.latitude),
    math.min(lowerLeft.longitude, upperRight.longitude))
  def rectangle2Polygon(r: Rectangle): Polygon = Polygon(r.getCorners)
}


/**
 * Boundingbox
 */
case class BoundingBox(maxLatitude: Double, maxLongitude: Double, minLatitude: Double, minLongitude: Double) extends Rectangle(maxLatitude, maxLongitude, minLatitude, minLongitude) {
  /** all sides are included as this is to search for possible inclusions */
  override def contains(p: Point) = super.mayContain(p)
}

object BoundingBox{
  def apply(p: Seq[Point]) = new BoundingBox(p.map(_.latitude).max, p.map(_.longitude).max, p.map(_.latitude).min, p.map(_.longitude).min)
}


/**
 * Circle
 * @param center - this assumes coordinates in degrees
 * @param radius_m - the radius is in meters
 */
case class Circle(center: Point, radius_m: Double) extends GeoArea {
  import funcs._

  override def getExtremes = pointsAtDistanceWithSameLatitude(center, radius_m) ++ pointsAtDistanceWithSameLongitude(center, radius_m)
  val boundingBox = BoundingBox(getExtremes)

  override def mayContain(p: Point): Boolean = boundingBox.contains(p)

  // distanceSphericalEarth is so much faster than distanceHaversine
  override def contains(p: Point): Boolean = distanceSphericalEarth(center, p) <= radius_m

  // using the simple SphericalEarth formula
  // 1.0000001 just making sure we're ever so slightly larger with the extremes
  import math.{cos, toRadians, toDegrees}
  def pointsAtDistanceWithSameLatitude(p: Point, dist_m: Double) = {
    val diff = toDegrees(1.0000001 * dist_m / (EarthRadius * cos(toRadians(p.latitude))))
    List(Point(p.latitude, p.longitude + diff), Point(p.latitude, p.longitude - diff))
  }

  def pointsAtDistanceWithSameLongitude(p: Point, dist_m: Double) = {
    val diff = toDegrees(1.0000001 * dist_m / EarthRadius)
    List(Point(p.latitude + diff, p.longitude), Point(p.latitude - diff, p.longitude))
  }
}

object Circle {
  def apply(center: Point, pointOnTheCircle: Point) = new Circle(center, center.distanceTo(pointOnTheCircle))
  def circle2Polygon(c: Circle, n: Int): Polygon = {
    import math.{toRadians, toDegrees, sin, cos, asin, atan2, Pi}
    import funcs._
    val lat1 = toRadians(c.center.latitude)
    val long1 = toRadians(c.center.longitude)
    val d_rad = c.radius_m / EarthRadius

    val points = (0 to n).map{i =>
      val radial = i * (2 * Pi)/n
      val lat_rad = asin(sin(lat1) * cos(d_rad) + cos(lat1) * sin(d_rad) * cos(radial))
      val dlon_rad = atan2(sin(radial) * sin(d_rad) * cos(lat1), cos(d_rad)- sin(lat1) * sin(lat_rad))
      val lon_rad = ((long1 + dlon_rad + Pi) % (2 * Pi)) - Pi
      Point(toDegrees(lat_rad), toDegrees(lon_rad))
    }
    new Polygon(points.toList, c.boundingBox)
  }
}

/**
 * Polygon
 */
case class Polygon(points: List[Point], boundingBox: BoundingBox) extends GeoArea {
  val edges = points.sliding(2).toList

  override def getExtremes = boundingBox.getExtremes

  override def mayContain(p: Point) = boundingBox.contains(p)

  // http://geomalgorithms.com/a03-_inclusion.html
  // even = out
  override def contains(p: Point) = {
    @inline def isOdd(i: Int) = i % 2 != 0
    @inline def crossesUpward(p: Point, e1: Point, e2: Point)   = (e1.longitude <= p.longitude) && (e2.longitude >  p.longitude)
    @inline def crossesDownward(p: Point, e1: Point, e2: Point) = (e1.longitude >  p.longitude) && (e2.longitude <= p.longitude)
    @inline def crossesTheLine(p: Point, e1: Point, e2: Point) = {
      if (crossesUpward(p, e1, e2) || crossesDownward(p, e1, e2)) {
        // compute the actual edge-ray intersect x-coordinate
        val ix = (p.longitude - e1.longitude) / (e2.longitude - e1.longitude)
        // if p.x < x intersext of edge with y = p.y
        p.latitude < e1.latitude + ix * (e2.latitude - e1.latitude)
      } else false
    }
    def countCrossings(p: Point): Int = edges.map{
      case p1 :: p2 :: Nil if crossesTheLine(p, p1, p2) => 1
      case _ => 0
    }.sum
    isOdd(countCrossings(p))
  }
}

object Polygon {
  def apply(points: List[Point]) = {
    require(points.length >= 3, "Polygon must have at least 3 points")
    // check if the last point is the same as first, if not, then ad it
    val p = if (points.head == points.last) points else points ++ List(points.head)
    val bb = BoundingBox(points)
    new Polygon(p, bb)
  }
}
