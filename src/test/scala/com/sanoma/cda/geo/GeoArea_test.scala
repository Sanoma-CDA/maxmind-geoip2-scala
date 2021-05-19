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

import org.scalatest.flatspec._
import org.scalatest.matchers._

class GeoArea_test extends AnyFlatSpec with should.Matchers {

  "GeoArea" should "simple rectangle" in {
    val rect = Polygon(List((0, 0), (1, 0), (1, 1), (0, 1)))
    rect.contains((0.5, 0.5)) shouldBe true
    rect.contains((0, 0)) shouldBe true
    rect.contains((1, 0)) shouldBe false // really?
    rect.contains((1, 1)) shouldBe false
    rect.contains((0, 1)) shouldBe false // really?

    rect.contains((0.5, 0.0)) shouldBe true
    rect.contains((1.0, 0.5)) shouldBe false
    rect.contains((0.5, 1.0)) shouldBe false
    rect.contains((0.0, 0.5)) shouldBe true
  }

  "GeoArea" should "multiple rectangles" in {
    val rects = List(
      Polygon(List((0, 0), (1, 0), (1, 1), (0, 1))), // 1
      Polygon(List((0, 1), (1, 1), (1, 2), (0, 2))), // 2
      Polygon(List((1, 1), (2, 1), (2, 2), (1, 2))), // 3 (1, 1) should belong to this? why not se same as above?
      Polygon(List((1, 0), (2, 0), (2, 1), (1, 1))) // 4
    )
    val answers = List(false, false, true, false)
    rects.map { r => r.contains((1.0, 1.0)) } shouldBe answers
  }

  "GeoArea" should "circle creation with 2 points" in {
    def pointsAtDistanceWithSameLongitude(p: Point, dist_m: Double) = {
      import funcs._

      import math.toDegrees
      val diff = toDegrees(dist_m / EarthRadius)
      List(Point(p.latitude + diff, p.longitude), Point(p.latitude - diff, p.longitude))
    }

    val testDist = 5000.0
    val points2 = pointsAtDistanceWithSameLongitude(Point(60.17, 24.94), testDist)
    val c0 = Circle(Point(60.17, 24.94), points2(0))
    c0.center shouldBe Point(60.17, 24.94)
    (math.abs(c0.radius_m - testDist) < (0.00001 * testDist)) shouldBe true
  }

  "GeoArea" should "circle simple inclusion" in {
    val helsinki = Point(60.17, 24.94)
    val tamminiemi = Point(60.1892, 24.8838)
    val mantyniemi = Point(60.1844, 24.8968)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    hCircle.contains(mantyniemi) shouldBe true
    hCircle.contains(tamminiemi) shouldBe false
  }

  "GeoArea" should "circle bounding box inclusion" in {
    val helsinki = Point(60.17, 24.94)
    val tamminiemi = Point(60.1892, 24.8838)
    val mantyniemi = Point(60.1844, 24.8968)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    hCircle.mayContain(mantyniemi) shouldBe true
    hCircle.mayContain(tamminiemi) shouldBe true
  }

  "GeoArea" should "Polygon creation" in {
    val p1 = Polygon(List((0, 0), (1, 0), (1, 1), (0, 1)))
    val p2 = Polygon(List((0, 0), (1, 0), (1, 1), (0, 1), (0, 0)))
    p1 shouldBe p2
  }

  "GeoArea" should "polygon simple inclusion" in {
    val poly = Polygon(List((-2.0, -2.0), (1.0, 5.0), (4.0, 1.0)))
    poly.contains((0, 0)) shouldBe true
    poly.contains((10, 10)) shouldBe false
  }

  "GeoArea" should "Polygon bounding box inclusion" in {
    val poly = Polygon(List((-2.0, -2.0), (1.0, 5.0), (4.0, 1.0)))
    poly.mayContain((0, 0)) shouldBe true
    poly.mayContain((-2, -2)) shouldBe true
    poly.mayContain((-2, 5)) shouldBe true
    poly.mayContain((4, 5)) shouldBe true
    poly.mayContain((4, -2)) shouldBe true

    poly.mayContain((-2.1, -2.1)) shouldBe false
    poly.mayContain((-2.1, 5.1)) shouldBe false
    poly.mayContain((4.1, 5.1)) shouldBe false
    poly.mayContain((4.1, -2.1)) shouldBe false
  }

  "GeoArea" should "Circle2Polygon" in {
    val helsinki = Point(60.17, 24.94)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    for (segments <- 3 to 60 by 5) {
      val polyCircle = Circle.circle2Polygon(hCircle, segments)
      polyCircle.points.map { t => math.abs(funcs.distanceHaversine(helsinki, t) - 3500) < 0.001 }.reduce(_ && _) shouldBe true
    }

    for (distances <- (10 to 1000 by 10) ++ (1000 to 500000 by 10000)) {
      val hCircle = Circle(helsinki, distances) // ??km around Helsinki
      val polyCircle = Circle.circle2Polygon(hCircle, 60)
      polyCircle.points.map { t => math.abs(funcs.distanceHaversine(helsinki, t) - distances) < 0.001 }.reduce(_ && _) shouldBe true
    }
  }
}
