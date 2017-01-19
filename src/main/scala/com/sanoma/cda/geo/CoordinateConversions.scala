/*
 * Copyright (c) 2016 Hugo Gävert. All rights reserved.
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

import math._

/**
  * This object contains functions to convert coordinates from one system another. However
  * it is not meant to be comprehensive library.
  * The focus has been to convert coordinates between
  * WGS84 / ETRS89 EUREF-FIN geographic 2D coordinates and ETRS-TM35FIN projected coordinates.
  *
  * For WGS84 coordinates, we use here the Point class that has in many functions been considered to be GPS coordinates.
  * For the projected plane coordinates, we use just a tuple of Doubles. If course, the implicit conversions make a
  * mess out of this. This may be corrected in the future.
  *
  */
object CoordinateConversions {
  /**
    * From Finnish plane coordinates to GPS coordinates.
    * @param etrs89PlaneCoordinates (northing, easting) both in meters
    * @return Point with GPS latitude and longitude
    */
  def etrs89tm35fin2wgs84(etrs89PlaneCoordinates: (Double, Double)): Point = {
    val wgspoint = ETRS89.toWGS84(etrs89PlaneCoordinates._1, etrs89PlaneCoordinates._2)
    Point(toDegrees(wgspoint._1), toDegrees(wgspoint._2))
  }

  /**
    * From GPS coordinates to Finnish projected plane coordinates
    * @param wgs84point Point with GPS latitude and longitude
    * @return (norting, easting) both in meters
    */
  def wgs842etrs89tm35fin(wgs84point: Point): (Double, Double) = {
    val etrspoint = ETRS89.toETRS89TM35FIN(toRadians(wgs84point._1), toRadians(wgs84point._2))
    (etrspoint._1, etrspoint._2)
  }

  @inline def asinh(r: Double) = log(r + sqrt(pow(r,2) + 1))
  @inline def atanh(r: Double) = 1/2.0 * log((1 + r) / (1 - r))
  @inline def sech(r: Double) = 1 / cosh(r)
  @inline def arsech(r: Double) = log((1 + sqrt(1 - pow(r,2))) / r)

  // what we are actually using is WGS84 which is pretty much the same
  object ETRS89 {
    // global conversion constants for WGS84 and ETRS89-TM35FIN
    // Conversion bounds from http://spatialreference.org/ref/epsg/etrs89-etrs-tm35fin/
    // WGS84 Bounds
    val minLat = toRadians(59.3)
    val maxLat = toRadians(70.13)
    val minLong = toRadians(19.09)
    val maxLong = toRadians(31.59)

    // ETRS-TM35FIN Projected Bounds
    val minN = 6582464.0358
    val maxN = 7799839.8902
    val minE = 50199.4814
    val maxE = 761274.6247

    // http://docs.jhs-suositukset.fi/jhs-suositukset/JHS154_liite1/JHS154_liite1.html
    // For WGS84:
    val a = 6378137.0          // ellipsoidin iisoakselin puolikas a (m)
    val b = 6356752.314245     // ellipsoidin pikkuakselin puolikas b (m)
    val f = 1/298.257223563    // ellipsoidin litistyssuhde f
    // http://www.maanmittauslaitos.fi/ammattilaisille/maastotiedot/koordinaatti-korkeusjarjestelmat/karttaprojektiot-tasokoordinaatistot/tasokoordinaatistot/etrs
    val k0 = 0.9996            // mittakaavakerroin keskimeridiaanilla
    val λ0 = toRadians(27.0)   // projektion keskimeridiaani
    val E0 = 500000.0          // Itäkoordinaatin arvo keskimeridiaanilla

    // Apusuureet
    val n = f / (2.0 - f) // (a - b) / (a + b)                                       // (01)
    val A1 = (a / (1.0 + n)) * (1 + pow(n, 2) / 4.0 + pow(n, 2) / 64.0)              // (02)
    val e2 = 2 * f - pow(f, 2)                                                       // (03)
    val e = sqrt(e2)
    val ep2 = pow(e, 2) / (1 - pow(e, 2))                                            // (04)
    val h1 = 1/2.0 * n - 2/3.0 * pow(n,2) + 37/96.0 * pow(n,3) - 1/360.0 * pow(n, 4) // (06)
    val h2 = 1/48.0 * pow(n, 2) + 1/15.0 * pow(n,3) - 437/1440.0 * pow(n,4)
    val h3 = 17/480.0 * pow(n, 3) - 37/840.0 * pow(n,4)
    val h4 = 4397/161280.0 * pow(n,4)
    val h1p = 1/2.0 * n - 2/3.0 * pow(n,2) + 5/16.0 * pow(n,3) + 41/180.0 * pow(n,4) // (07)
    val h2p = 13/48.0 * pow(n,2) - 3/5.0 * pow(n,3) + 557/1440.0 * pow(n,4)
    val h3p = 61/240.0 * pow(n,3) - 103/140.0 * pow(n,4)
    val h4p = 49561/161280.0 * pow(n,4)

    /**
      * http://docs.jhs-suositukset.fi/jhs-suositukset/JHS154_liite1/JHS154_liite1.html
      * @param φ in Radians
      * @param λ in Radians
      */
    def toETRS89TM35FIN(φ: Double, λ: Double) = {
      assert(φ >= minLat && φ <= maxLat, s"Latitude ${φ} is out of range($minLat, $maxLat)")
      assert(λ >= minLong && λ <= maxLong, s"Longitude ${λ} is out of range($minLong, $maxLong)")

      // Apusuureet
      val V2 = 1 + ep2 * pow(cos(φ), 2)         // (05)

      val Qp = asinh(tan(φ))                    // (08)
      val Qpp = atanh(e * sin(φ))               // (09)
      val Q = Qp - e * Qpp                      // (10)
      val l = λ - λ0                            // (11)
      val β = atan(sinh(Q))                     // (12)
      val ηp = atanh(cos(β) * sin(l))           // (13)
      val ξp = asin(sin(β) / sech(ηp))          // (14)
      val ξ1 = h1p * sin(2 * ξp) * cosh(2 * ηp) // (15)
      val ξ2 = h2p * sin(4 * ξp) * cosh(4 * ηp)
      val ξ3 = h3p * sin(6 * ξp) * cosh(6 * ηp)
      val ξ4 = h4p * sin(8 * ξp) * cosh(8 * ηp)
      val η1 = h1p * cos(2 * ξp) * sinh(2 * ηp) // (16)
      val η2 = h2p * cos(4 * ξp) * sinh(4 * ηp)
      val η3 = h3p * cos(6 * ξp) * sinh(6 * ηp)
      val η4 = h4p * cos(8 * ξp) * sinh(8 * ηp)
      val ξ = ξp + (ξ1 + ξ2 + ξ3 + ξ4)          // (17)
      val η = ηp + (η1 + η2 + η3 + η4)          // (18)
      val N = A1 * ξ * k0                       // (19)
      val E = A1 * η * k0 + E0                  // (20)
      (N, E)
    }

    // kind of admitting here that the Points are WGS84 and cannot be meters - it should be explicit in the other code as well
    /**
      * http://docs.jhs-suositukset.fi/jhs-suositukset/JHS154_liite1/JHS154_liite1.html
      * @param N in meters
      * @param E in meters (remember, without the "kaista")
      * @return Point in WGS84 (in Radians)
      */
    def toWGS84(N: Double, E: Double) = {
      assert(N >= minN && N <= maxN, s"${N}N is out of range($minN, $maxN)")
      assert(E >= minE && E <= maxE, s"${E}E is out of range($minE, $maxE)")

      // constants
      // φ geodeettinen leveys
      // λ geodeettinen pituus
      // E projektion itäkoordinaatti
      // N projektion pohjoiskoordinaatti
      // γ meridiaanikonvergenssi
      // k mittakaavakerroin
      // A1 meridiaanin pituisen ympyrän säde
      // e2 ensimmäisen epäkeskisyyden neliö
      // ep2 toisen epäkeskisyyden neliö
      // n toinen litistyssuhde
      // t suuntakulma tasolla (suuntakorjauksen kaavassa δ = T - t)
      // c napakaarevuussäde
      // M meridiaanikaarevuussäde
      // N poikittaiskaarevuussäde (kaavassa (42))

      val ξ = N / (A1 * k0)                   // (21)
      val η = (E - E0) / (A1 * k0)            // (22)

      val ξ1p = h1 * sin(2 * ξ) * cosh(2 * η) // (23)
      val ξ2p = h2 * sin(4 * ξ) * cosh(4 * η)
      val ξ3p = h3 * sin(6 * ξ) * cosh(6 * η)
      val ξ4p = h4 * sin(8 * ξ) * cosh(8 * η)
      val η1p = h1 * cos(2 * ξ) * sinh(2 * η) // (24)
      val η2p = h2 * cos(4 * ξ) * sinh(4 * η)
      val η3p = h3 * cos(6 * ξ) * sinh(6 * η)
      val η4p = h4 * cos(8 * ξ) * sinh(8 * η)

      val ξp = ξ - (ξ1p + ξ2p + ξ3p + ξ4p)    // (25)
      val ηp = η - (η1p + η2p + η3p + η4p)    // (26)

      val β = asin(sech(ηp) * sin(ξp))        // (27)  - double be = Math.asin(Math.sin(Ep) / Math.cosh(nnp));
      val l = asin(tanh(ηp) / cos(β))         // (28)

      val Q = asinh(tan(β))                   // (29)
      val Qp0 = Q + e * atanh(e * tanh(Q))    // (30)
      val Qp1 = Q + e * atanh(e * tanh(Qp0))  //iterointi, kunnes muutos = 0 // (31)
      val Qp2 = Q + e * atanh(e * tanh(Qp1))
      val Qp3 = Q + e * atanh(e * tanh(Qp2))
      val Qp4 = Q + e * atanh(e * tanh(Qp3))
      val Qp  = Q + e * atanh(e * tanh(Qp4))

      val φ = atan(sinh(Qp))                  // (32)
      val λ = λ0 + l                          // (33)
      (φ, λ)
    }
  }
}
