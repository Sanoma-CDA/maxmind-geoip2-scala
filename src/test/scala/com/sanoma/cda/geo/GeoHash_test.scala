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
 */package com.sanoma.cda.geo

import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import org.scalatest.Matchers._

import com.sanoma.cda.geo.GeoHash._

class GeoHash_test extends FunSuite with PropertyChecks {
  // let's create some test hashes
  val rand = scala.util.Random
  def randomHash(len: Int) = {
    val chars = decodeMap.keys.toList
    (1 to len).map(i => chars(rand.nextInt(chars.length - 1))).mkString("")
  }
  val hashLengths = (2 to 12).toList
  val nEachLength = 1000
  val geohashMax = 10 // from above
  val length1Hashes = List(base32.map(_.toString))
  val otherLengthHashes = hashLengths.map{l => (1 to nEachLength).toList.map(i => randomHash(l))}
  val differentLengthHashes = length1Hashes ++ otherLengthHashes

  // let's create some test points
  def roundDecimals(v: Double, d: Int) = math.round(v * math.pow(10, d)) / math.pow(10, d)
  def randDouble(min: Double, max: Double) = min + rand.nextDouble * (max - min) //roundDecimals(min + rand.nextDouble * (max - min),7)
  def randLatitude = randDouble(-90.0, 90.0)
  def randLongitude = randDouble(-180.0, 180.0)
  def randPoint = Point(randLatitude, randLongitude)
  val nPoints = 1000 // geoHashMax applies
  val pointsToTest = (1 to nPoints).map(i => randPoint)


  // Some functions to access geohash.org
  def decodeFromGeohashOrg(gh: Geohash): Point = {
    val expectedStr = scala.io.Source.fromURL(s"http://geohash.org/${gh}/text").getLines.toList
    expectedStr.head.split(" ") match {case Array(lat,long, _*) => Point(lat.toDouble, long.toDouble)}
  }
  def encodeFromGeohashOrg(len: Int)(point: Point): Geohash = {
    assert(len <= 12)
    val expectedStr = scala.io.Source.fromURL(s"http://geohash.org/?q=${point.latitude},${point.longitude}&format=url&redirect=0&maxlen=${len}").getLines.toList
    expectedStr.head.stripPrefix("http://geohash.org/")
  }


  // tests
  /**
   * This is for testing the rounding which is not the most obvious.
   * Most implementations seem to round incorrectly when compared to the wikipedia page.
   * "Final rounding should be done carefully in a way that
   *  min ≤ round(value) ≤ max"
   *
   */
  test("decode: test rounding") {
    val hashesToTest = differentLengthHashes.flatten
    // this is the rounded coordinates
    val hashesCalculated = hashesToTest.map{gh => decode(gh)}
    // from here we get the boundaries:
    val hashesFullPrecision = hashesToTest.map{gh => decodeFully(gh)}

    hashesToTest.zip(hashesCalculated.zip(hashesFullPrecision)) foreach {
      case (hash, (p, (lat, lon, late, lone))) => {
        val latmin = lat - late
        val latmax = lat + late
        val lonmin = lon - lone
        val lonmax = lon + lone
        if ((p.latitude >= latmin) & (p.latitude <= latmax) & (p.longitude >= lonmin) & (p.longitude <= lonmax)) {
          //println(s"rounding hash: $hash seems ok => $p should be in ($latmin - $latmax, $lonmin - $lonmax)")
        } else {
          println(s"rounding hash: $hash => $p should be in ($latmin - $latmax, $lonmin - $lonmax)")
        }
        p.latitude should be >= latmin
        p.latitude should be <= latmax
        p.longitude should be >= lonmin
        p.longitude should be <= lonmax
      }
    }
  }


  // Next tests test agains geohash.org
  // Unfortunately, it seems that geohash.org is
  // rounding values quite badly, therefore the epsilons
  // are quite large here.
  // But maybe these catch if the code completely breaks.


  /**
   * This test is testing decoding against geohash.org
   * Unfortunately, it seems that geohash.org is rounding incorrectly.
   * Therefore, the epsilon is quite large.
   */
  test("decode: test N against geohash.org") {
    val epsilons = List(1.0, 1.0, 1.0, 1.0, 1.0, 0.1, 0.01, 0.001, 0.0001, 0.0001, 0.00001, 0.000001)
    // NOTE: hashes with length 1 fail to get the same values as from geohash.org - often they get +1 degree
    // this is due the rounding in the precision function...
    for (hashesToTestAll <- differentLengthHashes) {
      val hashesToTest = hashesToTestAll.take(geohashMax)
      val len = hashesToTest(0).length
      val epsilon = epsilons(len-1)
      println(s"Testing hashes of length $len with epsilon $epsilon")
      val hashesExpected = hashesToTest.par.map{gh => decodeFromGeohashOrg(gh)}.seq
      val hashesCalculated = hashesToTest.map{gh => decode(gh)}
      hashesToTest.zip(hashesExpected.zip(hashesCalculated)) foreach {
        case (hash, (expected, calculated)) => {
          // we print these out as otherwise it's a bit hard to know what failed...
          if (expected != calculated) println(s"decoding hash: $hash => ${calculated}, geohash.org claims it to be ${expected}")
          math.abs(calculated._1-expected._1) should be <= epsilon
          math.abs(calculated._2-expected._2) should be <= epsilon
        }
      }
    }
  }

  /**
   * Testing encoding against geohash.org
   * Unfortunately, it seems that geohash.org also encodes incorrectly.
   */
  test("encode: test N against geohash.org") {
    val rand = scala.util.Random

    val precisionsToTest = 1 to 12

    for (precision <- precisionsToTest) {
      println(s"Testing precision $precision")
      val points = pointsToTest.take(geohashMax)
      val encodefromGeohashOrgWithPrecision = encodeFromGeohashOrg(precision) _
      val pointsExpected = points.par.map{ case p => encodefromGeohashOrgWithPrecision(p) }
      val pointsCalculated = points.par.map{ case p => encode(p, precision) }
      points.zip(pointsExpected.zip(pointsCalculated)) foreach {
        case (point, (expected, calculated)) => {
          if (expected != calculated) {
            print(s"encoding point: $point => ${calculated}, geohash.org gives ${expected}")
            //val pointFromGHO = decodeFromGeohashOrg(expected)
            val pointFromGHO = decode(expected) // at this point, we trust our own decode more
            val pointFromThis = decode(calculated)
            if (point.distanceTo(pointFromThis) <= point.distanceTo(pointFromGHO)) {
              //println(" - but they are further")
              println()
              point.distanceTo(pointFromThis) should be <= point.distanceTo(pointFromGHO)
            } else {
              println(" - And they might be right!") // but not necessarily
              if (precision < 12) { // then we check against our own high precision (which will be checked later on)
                val accurateThis = encode(point)
                val correctLettersGHO = accurateThis.zip(expected).map(t => t._1 == t._2).takeWhile(_ == true).size
                val correctLettersThis = accurateThis.zip(calculated).map(t => t._1 == t._2).takeWhile(_ == true).size
                correctLettersThis should be > correctLettersGHO
              } else {
                // So, yeah, could be that our high precision is further away, but still the difference should be small
                // BTW, distance is in meters, so the difference is really small here...
                math.abs(point.distanceTo(pointFromThis)-point.distanceTo(pointFromGHO)) should be <= 0.01
              }
            }

          } else {
            calculated shouldBe expected
          }
        }
      }
    }
  }

  test("encode - decode round-trip") {
    val precision = 12
    val epsilon = 1e-6
    val calculated = pointsToTest.par.map{p => decode(encode(p, precision))}
    pointsToTest.zip(calculated) foreach {
      case (orig, calc) => {
        if (orig != calc) println(s"encoding and decoding: $orig should be $calc")
        math.abs(calc._1 - orig._1) should be <= epsilon
        math.abs(calc._2 - orig._2) should be <= epsilon
      }
    }
  }

  test("decode - encode round-trip") {
    val hashesToTest = differentLengthHashes.flatten
    val calculated = hashesToTest.par.map{h =>
      val p = h.length
      val tmp = decodeFully(h) // full decode doesn't round, with rounding this probably won't work
      encode(Point(tmp._1, tmp._2), p)
    }
    hashesToTest.zip(calculated) foreach {
      case (orig, calc) => {
        if (orig != calc) println(s"decoding and encoding: $orig should be $calc")
        calc shouldBe orig
      }
    }
  }

  test("test longest common prefix") {
    val strs1 = List("interspecies","interstellar","interstate")
    val expected1 = "inters"
    longestCommonPrefix(strs1) shouldBe expected1

    val strs2 = List("foobar", "foobar")
    val expected2 = "foobar"
    longestCommonPrefix(strs2) shouldBe expected2

    val strs3 = List("foo", "Foo")
    val expected3 = ""
    longestCommonPrefix(strs3) shouldBe expected3

    for (r <- 1 to 100){
      println(r)
      val commonPrefix = randomHash(7)

      val hashes = (1 to 20).map(i => commonPrefix + i + randomHash(3))
      longestCommonPrefix(hashes) shouldBe commonPrefix
    }

  }


  test("smallesCommonGeohash") {
    val commonPrefix = randomHash(6)
    def randHash(seed: Int) = {
      commonPrefix + seed + randomHash(2 + rand.nextInt(4))
    }

    val hashes = (1 to 20).map(i => randHash(i))
    val points = hashes.map{h =>
      val (latMid, lonMid, latE, lonE) = decodeFully(h)
      Point(latMid, lonMid)
    }

    smallestCommonGeohash(points) shouldBe commonPrefix
  }

}
