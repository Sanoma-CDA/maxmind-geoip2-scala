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

object Point {
  import Numeric.Implicits._
  import language.implicitConversions
  implicit def tuple22Point[A](p: (A, A))(implicit n:Numeric[A]) = new Point(p._1.toDouble, p._2.toDouble)
  implicit def point2Tuple2(p: Point) = (p.latitude, p.longitude)

  // No implicit conversions between Point and Geohash
  import GeoHash._
  /**
   * This creates a new Point by decoding the given Geohash.
   */
  def fromGeohash(gh: Geohash) = decode(gh)

  /**
   * This creates new point using all the full decimals of the decoded geohash.
   * However, this loses the accuracy information and probably seems too accurate.
   */
  def fromGeohashPrecision(gh: Geohash) = {
    val tmp = decodeFully(gh)
    Point(tmp._1, tmp._2)
  }
}

/**
 * A geographical point
 * http://en.wikipedia.org/wiki/ISO_6709
 * x = latitude comes before y = longitude
 * height / depth optional
 * should have coordinate reference system, but we'll try to keep this simple (although, see circle)
 * @param latitude
 * @param longitude
 */
case class Point(latitude: Double, longitude: Double){
  import funcs._

  /**
    * Distance to another point
    * @param other
    * @return distance in meters
    */
  def distanceTo(other: Point) = distanceHaversine(this, other)

  /**
    * Creates a new point using given offset in degrees.
    * @param latitudeOffset
    * @param longitudeOffset
    * @return New point
    */
  def offsetDegrees(latitudeOffset: Double = 0.0, longitudeOffset: Double = 0.0) =
    Point(this.latitude + latitudeOffset, this.longitude + longitudeOffset)

  /**
    * Creates a new point that has been offset from the original one using meters.
    * Note: this moves the point given number of meters along both of the axis separately.
    * Uses simple algorithm that works ok for short distances.
    * @param latitudeOffsetMeters
    * @param longitudeOffsetMeters
    * @return New point
    */
  def offsetMeters(latitudeOffsetMeters: Double = 0.0, longitudeOffsetMeters: Double = 0.0) = {
    val latOffsetDeg = if (latitudeOffsetMeters == 0.0) 0.0 else latitudeOffsetInDeg(latitudeOffsetMeters)
    val longOffsetDeg = if (longitudeOffsetMeters == 0.0) 0.0 else longitudeOffsetInDeg(this.latitude, longitudeOffsetMeters)
    this.offsetDegrees(latOffsetDeg, longOffsetDeg)
  }

  import GeoHash._
  def geoHash: Geohash = geoHash(12)
  /**
   * Create the Geohash with given precision.
   * @param hashLength The length of desired geohash. Default 12, the most accurate.
   * @return Geohash
   */
  def geoHash(hashLength: Int = 12): Geohash = encode(this, hashLength)
}

object funcs {
  import math.{sin, cos, atan2, pow, toRadians, toDegrees, sqrt}

  val EarthRadius = 6371009.0 // meters, http://en.wikipedia.org/wiki/Earth_radius#Mean_radius

  /**
   * Distance using the simple pythagoran formula
   * Really useable only for short distances (what is short?) - but fast
   * // http://en.wikipedia.org/wiki/Geographical_distance#Spherical_Earth_projected_to_a_plane
   * @param p1
   * @param p2
   * @return Distance in meters
   */
  def distanceSphericalEarth(p1: Point, p2: Point) = {
    // phi = lat, lamda = long
    val dLatRad = toRadians(p2.latitude - p1.latitude)
    val dLngRad = toRadians(p2.longitude - p1.longitude)
    val meanLat = toRadians(p2.latitude + p1.latitude) / 2.0
    EarthRadius * sqrt(pow(dLatRad, 2) + pow(cos(meanLat) * dLngRad, 2))
  }

  /**
   * Distance using Haversine formula
   * http://en.wikipedia.org/wiki/Haversine_formula
   * @param p1
   * @param p2
   * @return Distance in meters
   */
  def distanceHaversine(p1: Point, p2: Point) = {
    @inline def haversin(phi: Double) = pow(sin(phi / 2.0), 2.0)
    val dLatRad = toRadians(p2.latitude - p1.latitude)
    val dLngRad = toRadians(p2.longitude - p1.longitude)
    val p1LatRad = toRadians(p1.latitude)
    val p2LatRad = toRadians(p2.latitude)
    val a = haversin(dLatRad) + cos(p1LatRad) * cos(p2LatRad) * haversin(dLngRad)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    EarthRadius * c
  }

  /**
    * Latitude offset in degrees for given distance
    * Note: not the most accurate, but fast - usable for short distances
    *
    * @param offsetMeters
    * @return Latitude offset in Degrees
    */
  def latitudeOffsetInDeg(offsetMeters: Double) =
    toDegrees(offsetMeters / EarthRadius)

  /**
    * Longitude offset in degrees for given distance at given latitude
    * Note: not the most accurate, but fast - usable for short distances
    *
    * @param latitudeDegrees
    * @param offsetMeters
    * @return Longitude offset in Degrees
    */
  def longitudeOffsetInDeg(latitudeDegrees: Double, offsetMeters: Double) =
    offsetMeters / ((EarthRadius * math.Pi / 180.0) * cos(toRadians(latitudeDegrees)))


}
