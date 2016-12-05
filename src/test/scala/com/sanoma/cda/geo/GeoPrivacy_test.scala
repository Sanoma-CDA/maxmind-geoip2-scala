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

class GeoPrivacy_test extends FunSuite with PropertyChecks {
  import GeoPrivacy._
  import funcs._
  import math.abs

  val helsinki = Point(60.17, 24.94)
  val oulu = Point(65.01, 25.47)
  val amsterdam = Point(52.37, 4.895)
  val cities = List(helsinki, oulu, amsterdam)

  test("randomUniform") {
    val limits = List((0.0, 1.0), (-2.3, -1.0), (-5.0, 5.0), (3.0, 14.0))
    for (lim <- limits) {
      val foo = Array.fill(100000){randomUniform(lim._1, lim._2)}
      abs(foo.min - lim._1) should be <= 0.01 * (lim._2 - lim._1)
      abs(foo.max - lim._2) should be <= 0.01 * (lim._2 - lim._1)
      abs(foo.sum / foo.size - (lim._2 + lim._1) / 2.0) should be <= 0.01 * (lim._2 - lim._1)
    }
  }

  test("randomNormal") {
    val limits = List((0.0, 1.0), (-2.3, 2.0), (-5.0, 5.0), (3.0, 14.0))
    for (lim <- limits) {
      val foo = Array.fill(100000){randomNormal(lim._1, lim._2)}
      abs(foo.sum / foo.size - lim._1) should be <= 0.1
    }
  }

  test("randomTruncatedNormal") {
    val limits = List((0.0, 1.0), (-2.3, 2.0), (-5.0, 5.0), (3.0, 14.0))
    for (lim <- limits) {
      val foo = Array.fill(100000){randomTruncatedNormal(lim._1, lim._2)}
      foo.min should be >= lim._1 - 2 * lim._2
      foo.max should be <= lim._1 + 2 * lim._2
      abs(foo.sum / foo.size - lim._1) should be <= 0.1
    }
  }

  test("additiveNoise") {
    for (c <- cities) {
      val newPoints = Array.fill(100000){ additiveUniformNoise(2.0, 4.0)(c) }
      val distances = newPoints.map(p => (p.latitude - c.latitude, p.longitude - c.longitude))
      val maxLat = distances.map(_._1).max
      val minLat = distances.map(_._1).min
      val maxLong = distances.map(_._2).max
      val minLong = distances.map(_._2).min
      //println((maxLat, minLat, maxLong, minLong))
      maxLat should be <= 2.0
      minLat should be >= -2.0
      maxLong should be <= 4.0
      minLong should be >= -4.0
    }
  }

  test("additiveNoiseMeters") {
    for (c <- cities) {
      val newPoints = Array.fill(100000){ additiveUniformNoiseMeters(200, 1000)(c)}
      val latDeg = latitudeOffsetInDeg(200)
      val longDeg = longitudeOffsetInDeg(c.latitude, 1000)
      // we need to check the distances separately
      val distancesLat = newPoints.map(p => distanceHaversine((p.latitude, c.longitude), c))
      val distancesLong = newPoints.map(p => distanceHaversine((c.latitude, p.longitude), c))
      //println((distancesLat.max, distancesLat.min, distancesLong.max, distancesLong.min))
      distancesLat.max should be <= 200.0
      distancesLong.max should be <= 1000.0

      val distances = newPoints.map(p => (p.latitude - c.latitude, p.longitude - c.longitude))
      val maxLat = distances.map(_._1).max
      val minLat = distances.map(_._1).min
      val maxLong = distances.map(_._2).max
      val minLong = distances.map(_._2).min
      //println((maxLat, minLat, maxLong, minLong))
      maxLat should be <= latDeg
      minLat should be >= -latDeg
      maxLong should be <= longDeg
      minLong should be >= -longDeg
    }
  }

  test("additiveGaussianNoise") {
    for (c <- cities) {
      val newPoints = Array.fill(100000){ additiveGaussianNoise(2.0)(c) }
      val distances = newPoints.map(p => (p.latitude - c.latitude, p.longitude - c.longitude))
      val maxLat = distances.map(_._1).max
      val minLat = distances.map(_._1).min
      val maxLong = distances.map(_._2).max
      val minLong = distances.map(_._2).min
      //println((maxLat, minLat, maxLong, minLong))
      maxLat should be <= 4.0
      minLat should be >= -4.0
      maxLong should be <= 4.0
      minLong should be >= -4.0
    }
  }

  test("additiveGaussianNoiseMeters") {
    for (c <- cities) {
      val newPoints = Array.fill(100000){ additiveGaussianNoiseMeters(200)(c) }
      val latDeg = latitudeOffsetInDeg(400)
      val longDeg = longitudeOffsetInDeg(c.latitude, 400)
      // we need to check the distances separately
      val distancesLat = newPoints.map(p => distanceHaversine((p.latitude, c.longitude), c))
      val distancesLong = newPoints.map(p => distanceHaversine((c.latitude, p.longitude), c))
      println((distancesLat.max, distancesLat.min, distancesLong.max, distancesLong.min))
      distancesLat.max should be <= 400.0
      distancesLong.max should be <= 400.0

      val distances = newPoints.map(p => (p.latitude - c.latitude, p.longitude - c.longitude))
      val maxLat = distances.map(_._1).max
      val minLat = distances.map(_._1).min
      val maxLong = distances.map(_._2).max
      val minLong = distances.map(_._2).min
      println((maxLat, minLat, maxLong, minLong))
      maxLat should be <= latDeg
      minLat should be >= -latDeg
      maxLong should be <= longDeg
      minLong should be >= -longDeg
    }
  }

  test("discretized") {
    val p = Point(1.234567, 3.4567890)
    discretize(0)(p) shouldBe Point(1, 3)
    discretize(1)(p) shouldBe Point(1.2, 3.5)
    discretize(2)(p) shouldBe Point(1.23, 3.46)
    discretize(3)(p) shouldBe Point(1.235, 3.457)
    discretize(4)(p) shouldBe Point(1.2346, 3.4568)
  }

  def countOccurances[T](list: Seq[T]): Map[T, Int] = list.groupBy{e: T => e}.mapValues(_.length)

  test("discretized 2") {
    for (c <- cities) {
      val mLat = 1
      val mLong = 2
      val newPoints = Array.fill(100000){ discretize(1)(additiveUniformNoise(mLat, mLong)(c)) }
      //newPoints foreach println
      val expectedNroOfPoints = (2 * mLat * 10 + 1) * (2 * mLong * 10 + 1)

      val uniquePoints = countOccurances(newPoints).keys.size
      println(uniquePoints + " / " + expectedNroOfPoints)
      uniquePoints shouldBe expectedNroOfPoints
    }
  }


}

