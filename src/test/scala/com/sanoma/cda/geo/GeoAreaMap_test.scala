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

class GeoAreaMap_test extends FunSuite with PropertyChecks {

  test("GeoAreaMap priority") {
    val data = List(
      "0" -> Polygon(List((0,0), (1,0), (1,1), (0,1))),
      "1" -> Polygon(List((0,0), (1,0), (1,1), (0,1))),
      "2" -> Polygon(List((0,0), (1,0), (1,1), (0,1)))
    )
    val gmap0 = GeoAreaMap.fromSeq(data)
    gmap0.get((0.5, 0.5)) shouldBe Some("0")

    val prio = Map("1" -> 5.0, "2" -> 2.0).withDefaultValue(0.0)
    val gmap1 = GeoAreaMap.fromSeq(data, prio)
    gmap1.get((0.5, 0.5)) shouldBe Some("1")
  }

  test("Map findOne") {
    val turku = Point(60.45, 22.25)
    val helsinki = Point(60.17, 24.94)
    val tamminiemi = Point(60.1892,24.8838)
    val mantyniemi = Point(60.1844,24.8968)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    val tCircle = Circle(tamminiemi, 1000)
    val hRectangle = Rectangle(lowerLeft = (60.15, 24.84), upperRight = (60.20, 25.00))
    val aPoly = Polygon(List((60.30, 24.88), (60.34, 24.95), (60.295, 25.02)))

    val data = List("tamminiemi" -> tCircle, "helsinki" -> hCircle, "airport" -> aPoly, "hRect" -> hRectangle)
    val gmap = GeoAreaMap.fromSeq(data)

    gmap.get(turku) shouldBe None
    gmap.get(mantyniemi) shouldBe Some("tamminiemi")
  }

  test("Map findMany") {
    val turku = Point(60.45, 22.25)
    val helsinki = Point(60.17, 24.94)
    val tamminiemi = Point(60.1892,24.8838)
    val mantyniemi = Point(60.1844,24.8968)
    val hCircle = Circle(helsinki, 3500) // 3.5km around Helsinki
    val tCircle = Circle(tamminiemi, 1000)
    val hRectangle = Rectangle(lowerLeft = (60.15, 24.84), upperRight = (60.20, 25.00))
    val aPoly = Polygon(List((60.30, 24.88), (60.34, 24.95), (60.295, 25.02)))

    val data = List("tamminiemi" -> tCircle, "helsinki" -> hCircle, "airport" -> aPoly, "hRect" -> hRectangle)
    val gmap = GeoAreaMap.fromSeq(data)

    gmap.getAll(turku) shouldBe List()
    gmap.getAll(mantyniemi) shouldBe List("tamminiemi", "helsinki", "hRect")
  }


  test("GeoAreaMap reading") {
    val strs =
      """1:0.0,0.0,0.0 1.0,0.0,0.0 1.0,1.0,0.0 0.0,1.0,0.0
        |2:0.0,1.0,0.0 1.0,1.0,0.0 1.0,2.0,0.0 0.0,2.0,0.0
        |3:1.0,1.0,0.0 2.0,1.0,0.0 2.0,2.0,0.0 1.0,2.0,0.0
        |4:1.0,0.0,0.0 2.0,0.0,0.0 2.0,1.0,0.0 1.0,1.0,0.0""".stripMargin.split("\n")
    def parser(s: String): Option[(String, Polygon)] = {
      val nameSplitter = """([^:]+):(.+)""".r
      val nameSplitter(name, data) = s
      val coordinateMatcher = """([+-]?\d+\.\d*),([+-]?\d+\.\d*),([+-]?\d+\.\d*) *""".r
      val poly = coordinateMatcher.findAllIn(data).matchData.map(m => m.subgroups).toList.map{
        l => Point(l(0).toDouble, l(1).toDouble)
      }
      Some((name, Polygon(poly)))
    }
    val geoMap = GeoAreaMap.fromStrIter(strs.toIterator, parser)

    geoMap.get((0.5, 0.5)) shouldBe Some("1")
    geoMap.get((1.0, 1.0)) shouldBe Some("3")
    geoMap.get((10.0, 10.0)) shouldBe None
    // They should still be the same... (had a bug here once
    geoMap.get((0.5, 0.5)) shouldBe Some("1")
    geoMap.get((1.0, 1.0)) shouldBe Some("3")
    geoMap.get((10.0, 10.0)) shouldBe None
  }

  test("GeoAreaMap reading from file") {
    val file = "src/test/resources/GeoAreaMap_polygons.txt"
    def parser(s: String): Option[(String, Polygon)] = {
      val nameSplitter = """([^:]+):(.+)""".r
      val nameSplitter(name, data) = s
      val coordinateMatcher = """([+-]?\d+\.\d*),([+-]?\d+\.\d*),([+-]?\d+\.\d*) *""".r
      val poly = coordinateMatcher.findAllIn(data).matchData.map(m => m.subgroups).toList.map{
        l => Point(l(0).toDouble, l(1).toDouble)
      }
      Some((name, Polygon(poly)))
    }
    val geoMap = GeoAreaMap.fromFile(file, parser)

    geoMap.get((0.5, 0.5)) shouldBe Some("1")
    geoMap.get((1.0, 1.0)) shouldBe Some("3")
    geoMap.get((10.0, 10.0)) shouldBe None
  }

}
