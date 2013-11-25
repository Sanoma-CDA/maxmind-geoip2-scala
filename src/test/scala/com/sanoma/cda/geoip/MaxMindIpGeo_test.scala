/*
 * Copyright (c) 2013 Sanoma Oyj. All rights reserved.
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
import org.scalatest.matchers.ShouldMatchers._
import java.net.InetAddress

class MaxMindIpGeo_test extends FunSuite with PropertyChecks {

  // to run tests, please download the Database to src/test/resources directory
  val MaxMindDB = "src/test/resources/GeoLite2-City.mmdb"

  test("getInetAddress") {
    val geo = MaxMindIpGeo(MaxMindDB, 0)
    geo.getInetAddress("123.123.123.123") should be === Some(InetAddress.getByName("123.123.123.123"))
    geo.getInetAddress("localhost").get.getHostAddress should be === "127.0.0.1"
    geo.getInetAddress("foo.bar") should be === None
  }

  // This data seemed to work on 2013-11-20... if the MaxMind data changes, these values can change
  val testData = Map(
    "213.52.50.8" -> // Norwegian IP address, provided by MaxMind in their test suite
      Some(IpLocation(
        countryCode = Some("NO"),
        countryName = Some("Norway"),
        region = None,
        city = None,
        latlon = Some((62,10)),
        postalCode = None,
        continent = Some("Europe")
      )),

    "128.232.0.0" -> // Cambridge uni address, taken from http://www.ucs.cam.ac.uk/network/ip/camnets.html
      Some(IpLocation(
        countryCode = Some("GB"),
        countryName = Some("United Kingdom"),
        region = Some("Cambridgeshire"),
        city = Some("Cambridge"),
        latlon = Some((52.2, 0.1167)),
        postalCode = None,
        continent = Some("Europe")
      )),

    "4.2.2.2" -> // Famous DNS server, taken from http://www.tummy.com/articles/famous-dns-server/
      Some(IpLocation(
        countryCode = Some("US"),
        countryName = Some("United States"),
        region = None,
        city = None,
        latlon = Some((38.0, -97.0)),
        postalCode = None,
        continent = Some("North America")
      )),

    "194.60.0.0" -> // UK Parliament, taken from http://en.wikipedia.org/wiki/Wikipedia:Blocking_IP_addresses
      Some(IpLocation(
        countryCode = Some("GB"),
        countryName = Some("United Kingdom"),
        region = None,
        city = None,
        latlon = Some((51.5, -0.13)),
        postalCode = None,
        continent = Some("Europe")
      )),

    "192.0.2.0" -> // Invalid IP address, as per http://stackoverflow.com/questions/10456044/what-is-a-good-invalid-ip-address-to-use-for-unit-tests
      None
  )

  test("getLocationWithoutLruCache") {
    for ((address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, 0)
      geo.getLocationWithoutLruCache(address) should be === expected
    }
  }

  test("getLocationWithLruCache") {
    val cacheSize = List(1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache)
      geo.getLocationWithLruCache(address) should be === expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache)
      geo.getLocationWithLruCache(address) should be === expected
    }
  }

  test("getLocation") {
    val cacheSize = List(0, 1000, 10000)

    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache)
      geo.getLocation(address) should be === expected
    }

    // again from the cache
    for (cache <- cacheSize; (address, expected) <- testData) {
      val geo = MaxMindIpGeo(MaxMindDB, cache)
      geo.getLocation(address) should be === expected
    }
  }

}
