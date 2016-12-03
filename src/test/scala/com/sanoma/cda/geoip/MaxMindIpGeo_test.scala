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

import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import org.scalatest.Matchers._
import java.net.InetAddress

import com.sanoma.cda.geo._

class MaxMindIpGeo_test extends FunSuite with PropertyChecks {

  // to run tests, please download the Database to src/test/resources directory
  val MaxMindDB = "src/test/resources/GeoLite2-City.mmdb"

  test("getInetAddress") {
    val geo = MaxMindIpGeo(MaxMindDB, 0)
    geo.getInetAddress("123.123.123.123") shouldBe Some(InetAddress.getByName("123.123.123.123"))
    geo.getInetAddress("localhost").get.getHostAddress shouldBe "127.0.0.1"
    geo.getInetAddress("foo.bar.baz") shouldBe None
  }

  // This data seemed to work on 2013-11-20... if the MaxMind data changes, these values can change
  val testData = Map(
    "213.52.50.8" -> // Norwegian IP address, provided by MaxMind in their test suite
      Some(IpLocation(
        countryCode = Some("NO"),
        countryName = Some("Norway"),
        region = Some("Oslo County"),
        city = Some("Oslo"),
        geoPoint = Some(Point(59.9167,10.75)),
        postalCode = Some("6455"),
        continent = Some("Europe")
      )),

    "128.232.0.0" -> // Cambridge uni address, taken from http://www.ucs.cam.ac.uk/network/ip/camnets.html
      Some(IpLocation(
        countryCode = Some("GB"),
        countryName = Some("United Kingdom"),
        region = Some("Cambridgeshire"),
        city = Some("Cambridge"),
        geoPoint = Some(Point(52.2,0.1167)),
        postalCode = Some("CB5"),
        continent = Some("Europe")
      )),

    "4.2.2.2" -> // Famous DNS server, taken from http://www.tummy.com/articles/famous-dns-server/
      Some(IpLocation(
        countryCode = Some("US"),
        countryName = Some("United States"),
        region = None,
        city = None,
        geoPoint = Some(Point(37.751, -97.822)),
        postalCode = None,
        continent = Some("North America")
      )),

    "194.60.0.0" -> // UK Parliament, taken from http://en.wikipedia.org/wiki/Wikipedia:Blocking_IP_addresses
      Some(IpLocation(
        countryCode = Some("GB"),
        countryName = Some("United Kingdom"),
        region = None,
        city = None,
        geoPoint = Some(Point(51.4964, -0.1224)),
        postalCode = None,
        continent = Some("Europe")
      )),

    "192.0.2.0" -> // Invalid IP address, as per http://stackoverflow.com/questions/10456044/what-is-a-good-invalid-ip-address-to-use-for-unit-tests
      None
  )

  test("getLocationWithoutLruCache") {
    for ((address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, 0, synchronized = false)
      geo.getLocationWithoutLruCache(address) shouldBe expected
    }
  }

  test("getLocationWithLruCache") {
    val cacheSize = List(1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = false)
      geo.getLocationWithLruCache(address) shouldBe expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = false)
      geo.getLocationWithLruCache(address) shouldBe expected
    }
  }

  test("getLocation") {
    val cacheSize = List(0, 1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = false)
      geo.getLocation(address) shouldBe expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = false)
      geo.getLocation(address) shouldBe expected
    }
  }

  test("getLocationWithoutLruCache - sync") {
    for ((address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, 0, synchronized = true)
      geo.getLocationWithoutLruCache(address) shouldBe expected
    }
  }

  test("getLocationWithLruCache - sync") {
    val cacheSize = List(1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = true)
      geo.getLocationWithLruCache(address) shouldBe expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = true)
      geo.getLocationWithLruCache(address) shouldBe expected
    }
  }

  test("getLocation - sync") {
    val cacheSize = List(0, 1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = true)
      geo.getLocation(address) shouldBe expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache, synchronized = true)
      geo.getLocation(address) shouldBe expected
    }
  }

  test("postfilter - Remove point if is' on blacklist") {
    // create function for mapping/filtering
    // blacklist the first geo coordinate:
    // (59.95,10.75)
    val removeIncorrectLatLong: MaxMindIpGeo.IpLocationFilter = loc => {
      val geoPointBlacklist = Set(Point(37.751, -97.822))
      loc.geoPoint match {
        case Some(p) if geoPointBlacklist.contains(p) => Some(loc.copy(geoPoint = None))
        case _ => Some(loc)
      }
    }

    // Create the geo object with the filter-function
    val geo = MaxMindIpGeo(MaxMindDB, 0, synchronized = false, removeIncorrectLatLong)

    // check the changed
    val expected = Some(IpLocation(
      countryCode = Some("US"),
      countryName = Some("United States"),
      region = None,
      city = None,
      geoPoint = None,
      postalCode = None,
      continent = Some("North America")
    ))

    // Use the normal way
    geo.getLocation("4.2.2.2") shouldBe expected

    // others should still be fine:
    for ((address, expected) <- testData.filterKeys{k => k != "4.2.2.2"}) {
      geo.getLocationWithoutLruCache(address) shouldBe expected
    }
  }

  test("postfilter - Remove point if no city") {
    // create function for mapping/filtering
    // Check the city, if it's missing, just throw away the lat,long
    def noPointIfNoCity(loc: IpLocation) = {
      loc.city match {
        case Some(c) => Some(loc)
        case None => Some(loc.copy(geoPoint = None))
      }
    }

    val expected = Some(IpLocation(
      countryCode = Some("US"),
      countryName = Some("United States"),
      region = None,
      city = None,
      geoPoint = None,
      postalCode = None,
      continent = Some("North America")
    ))

    // Use the normal way
    val geo = MaxMindIpGeo(MaxMindDB, 0, postFilterIpLocation = noPointIfNoCity)
    geo.getLocation("4.2.2.2") shouldBe expected

  }

  test("postfilter - None if no city") {
    // create function for mapping/filtering
    // Check the city, if it's missing then throw away everything
    val noneIfNoCity: MaxMindIpGeo.IpLocationFilter = loc => {
      loc.city match {
        case Some(c) => Some(loc)
        case None => None
      }
    }

    // Use the normal way
    val geo = MaxMindIpGeo(MaxMindDB, 0, postFilterIpLocation = noneIfNoCity)
    geo.getLocation("4.2.2.2") shouldBe None
    
  }



}
