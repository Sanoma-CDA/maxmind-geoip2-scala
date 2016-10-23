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

import com.maxmind.geoip2.model.CityResponse
import com.sanoma.cda.geo._

/**
 * Case class to hold the location information from MaxMind.
 */
case class IpLocation(
                       countryCode: Option[String],
                       countryName: Option[String],
                       region: Option[String], // should look more into this...
                       city: Option[String],
                       geoPoint: Option[Point],
                       postalCode: Option[String],
                       continent: Option[String])

// MaxMind
import com.maxmind.geoip2.model.CityResponse

/**
 * Companion object to help convert MaxMind OmniResponse to IpLocation
 */
object IpLocation {

  // Doesn't make sense to only have Latitude or Longitude
  def combineLatLong(lat: Option[Double], lon: Option[Double]) = (lat, lon) match {
    case (Some(lat), Some(lon)) => Some(Point(lat,lon))
    case _ => None
  }

  /**
   * Constructs an IpLocation from a MaxMind Location
   */
  def apply(omni: CityResponse): IpLocation = new IpLocation(
    Option(omni.getCountry).map(_.getIsoCode),
    Option(omni.getCountry).map(_.getName),
    Option(omni.getMostSpecificSubdivision).map(_.getName),
    Option(omni.getCity).map(_.getName),
    Option(omni.getLocation).flatMap { location => combineLatLong(Option(location.getLatitude), Option(location.getLongitude)) },
    Option(omni.getPostal).map(_.getCode),
    Option(omni.getContinent).map(_.getName)
  )
}
