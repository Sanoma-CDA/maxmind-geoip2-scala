/*
 * Copyright (c) 2015 Hugo Gävert. All rights reserved.
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

/**
 * This object provides basic Geohash encoding and decoding.
 * Geohash web-site: http://geohash.org/
 * Geohash description on Wikipedia: https://en.wikipedia.org/wiki/Geohash
 *
 * This implementation is very simple and mostly modelled after
 * Python version at https://github.com/vinsci/geohash/blob/master/Geohash/geohash.py
 */
object GeoHash {
  type Geohash = String

  // geohash uses it's own base32 - from Wikipedia
  val base32 = "0123456789bcdefghjkmnpqrstuvwxyz".toList
  val decodeMap = base32.zipWithIndex.toMap
  val bitMask = Array(16, 8, 4, 2, 1)
  
  @inline def mid(interval: (Double, Double)) = (interval._1 + interval._2) / 2.0

  /**
   * This function decodes fully the given Geohash string
   * @param geohash - Geohash string
   * @return - Tuple with (latitude, longitude, latitude ±error, longitude ±error)
   */
  def decodeFully(geohash: Geohash): (Double, Double, Double, Double) = {
    // latitude and longitude (interval, error)
    var (latI, latE) = ((-90.0, 90.0), 90.0)
    var (lonI, lonE) = ((-180.0, 180.0), 180.0)
    var isEven = true

    for (c <- geohash.toCharArray) {
      val cd = decodeMap(c)
      for (mask <- bitMask) {
        if (isEven) { // longitude
          lonE = lonE / 2.0
          lonI = if ((cd & mask) > 0) (mid(lonI), lonI._2) else (lonI._1, mid(lonI))
        } else { // latitude
          latE = latE / 2.0
          latI = if ((cd & mask) > 0) (mid(latI), latI._2) else (latI._1, mid(latI))
        }
        isEven = !isEven
      }
    }
    (mid(latI), mid(lonI), latE, lonE)
  }

  def geoHash2Rectangle(geohash: Geohash): Rectangle = {
    val (latMid, lonMid, latE, lonE) = decodeFully(geohash)
    new Rectangle(latMid + latE, lonMid + lonE, latMid - latE, lonMid - lonE)
  }

  /**
   * This rounds the coordinates to desired precision. See wikipedia for rounding.
   * @param x Value to round
   * @param xError ±error for x
   * @return Rounded value
   */
  def getRounded(x: Double, xError: Double) = {
    import math._
    val xmin = x - xError
    val xmax = x + xError
    //println(s"x=$x => [$xmin, $xmax]")
    var xPrecision: Double = max(1, round(-log10(xError))) - 1
    var decimals: Double = pow(10, xPrecision)
    var rounded = round(x * decimals) / decimals
    //println(s"rounded = $rounded, decimals = $decimals")
    while ((rounded < xmin) | (rounded > xmax)) {
      xPrecision += 1.0
      decimals = pow(10, xPrecision)
      rounded = round(x * decimals) / decimals
      //println(s"rounded = $rounded, decimals = $decimals")
    }
    rounded
  }

  /**
   * Just decode and get the point. Rounds the precisions also
   * @param gh Geohash to decode
   * @return The point
   */
  def decode(gh: Geohash): Point = {
    val (lat, lon, latErr, lonErr) = decodeFully(gh)
    Point(getRounded(lat, latErr), getRounded(lon, lonErr))
  }

  /**
   * Encoding function
   * @param point Point
   * @param hashLength Desired length of the hash
   * @return The geohash string
   */
  def encode(point: Point, hashLength: Int = 12): Geohash = {
    var latI = (-90.0, 90.0)
    var lonI = (-180.0, 180.0)
    var isEven = true
    var geohash = new StringBuilder
    var bit = 0
    var ch = 0

    while (geohash.length < hashLength) {
      if (isEven) { // longitude
        val midPoint = mid(lonI)
        if (point.longitude > midPoint) {
          ch = ch | bitMask(bit)
          lonI = (midPoint, lonI._2)
        } else lonI = (lonI._1, midPoint)
      } else { // latitude
        val midPoint = mid(latI)
        if (point.latitude > midPoint) {
          ch = ch | bitMask(bit)
          latI = (midPoint, latI._2)
        } else latI = (latI._1, midPoint)
      }
      isEven = !isEven
      if (bit < 4) bit = bit + 1
      else {
        geohash += base32(ch)
        bit = 0
        ch = 0
      }
    }
    geohash.toString
  }

  /**
    * This function just returns the longest common prefix of the sequence of strings.
    * @param strs Sequence of the strings
    * @return The longest common prefix
    */
  def longestCommonPrefix(strs: Seq[String]): String = {
    val charA = strs.map(_.toCharArray)
    val maxLength = charA.map(_.length).min
    (0 until maxLength).view
      .map{i => charA.map(_(i)).toSet}
      .takeWhile(_.size == 1)
      .map(_.head).mkString
  }

  /**
    * This tries to find the smallest geohash that contains all the points.
    * It does it by encoding all points to highest precision and looks for the
    * longest common prefix of the geohash. However, due the way geohash is calculated
    * this may return very large areas. If this is long or "accurate", the points are all
    * in close proximity. However, this may be very large area even if the points are
    * all from very small region that happens to cross large geohash borders.
    * @param points List of points
    * @return The smalles common Geohash
    */
  def smallestCommonGeohash(points: Seq[Point]): Geohash =
    longestCommonPrefix(points.map(p => encode(p)))
}
