package com.sanoma.cda.geoip

import com.sanoma.cda.geo._
import com.sanoma.cda.geo.model._
import collection.JavaConverters._

/**
 * Simplified IpLocation information
 */

abstract class LocationLike

case class IpLocation(
  countryCode: Option[String] = None,
  countryName: Option[String] = None,
  region:      Option[String] = None, // should look more into this...
  city:        Option[String] = None,
  geoPoint:    Option[Point] = None,
  postalCode:  Option[String] = None,
  continent:   Option[String] = None,
  metroCode:   Option[Int] = None
) extends LocationLike

object IpLocation {

  def apply(richIpLocation: RichIpLocation): IpLocation = new IpLocation(
    richIpLocation.country.map(_.isoCode),
    richIpLocation.country.map(_.name),
    richIpLocation.region.map(_.name),
    richIpLocation.city.map(_.name),
    richIpLocation.geoPoint,
    richIpLocation.postalCode.map(_.code),
    richIpLocation.continent.map(_.name),
    richIpLocation.location.flatMap(_.metroCode)
  )

}

/**
 * Case class to hold the location information from MaxMind.
 */
case class RichIpLocation(
  country:      Option[Country] = None,
  subdivisions: Option[List[Subdivision]] = None, // should look more into this...
  city:         Option[City] = None,
  geoPoint:     Option[Point] = None,
  postalCode:   Option[Postal] = None,
  continent:    Option[Continent] = None,
  location:     Option[Location] = None
) extends LocationLike {

  def region: Option[Subdivision] = subdivisions.flatMap(_.lastOption)

}

// MaxMind
import com.maxmind.geoip2.model.CityResponse

/**
 * Companion object to help convert MaxMind OmniResponse to IpLocation
 */
object RichIpLocation {

  /**
   * Constructs a RichIpLocation from a MaxMind Location
   */
  def apply(omni: CityResponse): RichIpLocation = new RichIpLocation(
    if (omni.getCountry.getGeoNameId != null) Option(omni.getCountry).map(Country(_)) else None,
    if (omni.getSubdivisions.size != 0) Option(omni.getSubdivisions.asScala.toList.map(Subdivision(_))) else None,
    if (omni.getCity.getGeoNameId != null) Option(omni.getCity).map(City(_)) else None,
    if (omni.getLocation != null) Point.combineLatLong(Point.jDoubleOptionify(omni.getLocation.getLatitude), Point.jDoubleOptionify(omni.getLocation.getLongitude)) else None,
    if (omni.getPostal.getCode != null) Option(omni.getPostal).map(Postal(_)) else None,
    if (omni.getContinent.getCode != null) Option(omni.getContinent).map(Continent(_)) else None,
    if (omni.getLocation != null) Option(omni.getLocation).map(Location(_)) else None
  )
}