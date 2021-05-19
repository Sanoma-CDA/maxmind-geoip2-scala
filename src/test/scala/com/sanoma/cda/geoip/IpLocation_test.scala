/*
 * Copyright (c) 2013-2014 Sanoma Oyj. All rights reserved.
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
package com.sanoma.cda.geoip

import com.sanoma.cda.geo.Point
import org.scalatest.flatspec._
import org.scalatest.matchers._


class IpLocation_test extends AnyFlatSpec with should.Matchers {

  "IpLocation" should "jDoubleOptionify" in {
    val jNull: java.lang.Double = null
    val jNull_expected: Option[Double] = None
    IpLocation.jDoubleOptionify(jNull) shouldBe jNull_expected

    val jOk: java.lang.Double = 2.5
    val jOk_expected: Option[Double] = Some(2.5)
    IpLocation.jDoubleOptionify(jOk) shouldBe jOk_expected
  }

  "IpLocation" should "combineLatLong" in {
    IpLocation.combineLatLong(None, None) shouldBe None
    IpLocation.combineLatLong(Some(2.5), None) shouldBe None
    IpLocation.combineLatLong(None, Some(3.6)) shouldBe None
    IpLocation.combineLatLong(Some(2.5), Some(3.6)) shouldBe Some(Point(2.5, 3.6))
  }

  "IpLocation" should "implicit conversions" in {
    def Point2List(p: Point): List[Double] = List(p.latitude, p.longitude)

    def Tuple2List(p: (Double, Double)): List[Double] = List(p._1, p._2)

    val p1 = Point(62, 10)
    val t2 = (62.0, 10.0)

    Point2List(p1) shouldBe Tuple2List(t2)
    Point2List(t2) shouldBe Tuple2List(p1)
  }

}
