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

import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import org.scalatest.Matchers._

class Point_test extends FunSuite with PropertyChecks {

  // a few points
  val turku = Point(60.45, 22.25)
  val helsinki = Point(60.17, 24.94)
  val tamminiemi = Point(60.1892,24.8838)
  val mantyniemi = Point(60.1844,24.8968)

  // a few reference distances
  // Wolfram Alpha apparently uses the more accurate Vincenty method
  val distAccordingToWolfram = Map(
    (turku, helsinki) -> 151921.0,
    (tamminiemi, mantyniemi) -> 897.94
  )
  // http://www.movable-type.co.uk/scripts/latlong.html
  val distAccordingToMovableType = Map(
    (turku, helsinki) -> 151400.0, //151.4km, unfortunately doesn't give more accurate output
    (tamminiemi, mantyniemi) -> 895.2 // 0.8952 km
  )
  // http://www.onlineconversion.com/map_greatcircle_distance.htm
  // Claims to use haversine
  val distAccordingToOnlineConv = Map(
    (turku, helsinki) -> 151378.06036313538,
    (tamminiemi, mantyniemi) -> 895.1967362650147
  )


  def distError(value: Double, reference: Double) = math.abs(reference - value) / reference
  def distCheck(f: (Point, Point) => Double, pair: (Point, Point), ref: Map[(Point, Point), Double]) = distError(f(pair._1, pair._2), ref(pair))

  test("distanceSphericalEarth") {
    import funcs._
    distCheck(distanceSphericalEarth, (tamminiemi, mantyniemi), distAccordingToWolfram) should be < 0.01
    distCheck(distanceSphericalEarth, (turku, helsinki), distAccordingToWolfram) should be < 0.01

    distCheck(distanceSphericalEarth, (tamminiemi, mantyniemi), distAccordingToMovableType) should be < 0.0001
    distCheck(distanceSphericalEarth, (turku, helsinki), distAccordingToMovableType) should be < 0.0001

    distCheck(distanceSphericalEarth, (tamminiemi, mantyniemi), distAccordingToOnlineConv) should be < 0.00001
    distCheck(distanceSphericalEarth, (turku, helsinki), distAccordingToOnlineConv) should be < 0.0001 // Not as accurate
  }

  test("distanceHaversine") {
    import funcs._
    distCheck(distanceHaversine, (tamminiemi, mantyniemi), distAccordingToWolfram) should be < 0.01
    distCheck(distanceHaversine, (turku, helsinki), distAccordingToWolfram) should be < 0.01

    distCheck(distanceHaversine, (tamminiemi, mantyniemi), distAccordingToMovableType) should be < 0.001
    distCheck(distanceHaversine, (turku, helsinki), distAccordingToMovableType) should be < 0.001

    distCheck(distanceHaversine, (tamminiemi, mantyniemi), distAccordingToOnlineConv) should be < 0.00001
    distCheck(distanceHaversine, (turku, helsinki), distAccordingToOnlineConv) should be < 0.00001
  }

  test("geohash conversions") {
    // Full geohash tests in GeoHash_test.scala
    tamminiemi.geoHash shouldBe "ud9wqjpgd845"
    tamminiemi.geoHash(9) shouldBe "ud9wqjpgd"
    Point.fromGeohash("ud9wqjpgd845") shouldBe tamminiemi
    Point.fromGeohashPrecision("ud9wqjpgd845") shouldBe Point(60.18920003436506,24.883800018578768)
  }

  test("jDoubleOptionify") {
    val jNull: java.lang.Double = null
    val jNull_expected: Option[Double] = None
    Point.jDoubleOptionify(jNull) shouldBe jNull_expected

    val jOk: java.lang.Double = 2.5
    val jOk_expected: Option[Double] = Some(2.5)
    Point.jDoubleOptionify(jOk) shouldBe jOk_expected
  }

  test("combineLatLong") {
    Point.combineLatLong(None, None) shouldBe None
    Point.combineLatLong(Some(2.5), None) shouldBe None
    Point.combineLatLong(None, Some(3.6)) shouldBe None
    Point.combineLatLong(Some(2.5), Some(3.6)) shouldBe Some(Point(2.5, 3.6))
  }

  test("implicit conversions") {
    def Point2List(p: Point): List[Double] = List(p.latitude, p.longitude)
    def Tuple2List(p: (Double, Double)): List[Double] = List(p._1, p._2)

    val p1 = Point(62,10)
    val t2 = (62.0,10.0)

    Point2List(p1) shouldBe Tuple2List(t2)
    Point2List(t2) shouldBe Tuple2List(p1)
  }

}
