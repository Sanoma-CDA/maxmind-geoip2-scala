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

/**
  * This object provides funtions for alteringing the precision
  * of the geographical location to preserve users' privacy.
  */
object GeoPrivacy {
  type PointTransferFunction = Point => (Point, Option[Rectangle])

  /**
    * Generic transfer function that takes as parameter separate transfer functions
    * for latitude and longitude.
    * @param transferFunLat The function that transfers latitude
    * @param transferFunLong The function that transfers longitude
    * @param p
    * @return new point
    */
  def pointTransformerGen(transferFunLat: Double => Double, transferFunLong: Double => Double)(p: Point) =
    Point(transferFunLat(p.latitude), transferFunLong(p.longitude))

  /**
    * Here is one example of transferFunction. This rounds the
    * lotitude or longitude to given number of decimals.
    * @param decimals Desired number of decimals
    * @param d Original value
    * @return New value
    */
  def transformRoundAt(decimals: Int)(d: Double) = {
    val s = math.pow(10, decimals)
    math.round(d * s) / s
  }

  /**
    * This function returns new point where the latitude and longitude
    * degrees have been rounded up to given decimals. In effect, this creates
    * discretized grid to which all locations are mapped to.
    * This is deterministic - same location will always end up with the same new value.
    * @param decimals Number of decimals for the new accuracy of the location.
    *                 Same value is used for latitude and longitude.
    * @param p The original point to be rounded
    * @return The new location Point
    */
  def discretized(decimals: Int)(p: Point): Point = {
    val transformLat = transformRoundAt(decimals) _
    val transformLong = transformRoundAt(decimals) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  val randGen = new scala.util.Random

  /**
    * Transfer function that adds uniform noise to the value.
    * @param maxDiff Max positive and negative difference that can happen (in degrees)
    * @param d Original value
    * @return New value
    */
  def transformUniformNoiseGen(maxDiff: Double)(d: Double): Double =
    d + 2 * maxDiff * randGen.nextDouble - maxDiff

  /**
    * This function returns a new point where the location has been moved by
    * adding uniform noise to it.
    * This function is nondeterministic, different value each time.
    * @param noiseLatMax
    * @param noiseLongMax
    * @param p
    * @return
    */
  def additiveNoise(noiseLatMax: Double, noiseLongMax: Double)(p: Point) = {
    val transformLat = transformUniformNoiseGen(noiseLatMax) _
    val transformLong = transformUniformNoiseGen(noiseLongMax) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  /**
    * Transfer function that adds Gaussian noise to the value.
    * @param mean Mean value of the Gaussian noise
    * @param std Standard deviation of the Gaussian noise to be added
    * @param d Original value
    * @return New value
    */
  def transformGaussianNoiseGen(mean: Double = 0.0, std: Double = 1.0)(d: Double): Double =
    d + std * randGen.nextGaussian + mean

  /**
    * This function returns new point so that the location has been moved by
    * adding Gaussian noise to it. The parameters are shared for latitude and longitude.
    * This function is nondeterministic, different value each time.
    * @param std Standard deviation of the Gaussian noise to be added
    * @param mean Mean value of the Gaussian noise (default 0)
    * @param p Original point
    * @return New point
    */
  def additiveGaussianNoise(mean: Double = 0.0, std: Double = 1.0)(p: Point) = {
    val transformLat = transformGaussianNoiseGen(mean, std) _
    val transformLong = transformGaussianNoiseGen(mean, std) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  def midPointOfGeoHashGen(hashLength: Int)(p: Point) =
    GeoHash.decode(p.geoHash(hashLength))

}
