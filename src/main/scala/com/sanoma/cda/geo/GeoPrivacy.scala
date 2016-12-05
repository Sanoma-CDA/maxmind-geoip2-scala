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

import com.sanoma.cda.geo.funcs._
import math.{round, pow, abs}


/**
  * This object provides funtions for altering the precision
  * of the geographical location to preserve users' privacy.
  */
object GeoPrivacy {

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
  def roundAt(decimals: Int)(d: Double) = {
    val s = pow(10, decimals)
    round(d * s) / s
  }

  /**
    * Here is another transferFunction. This rounds the given
    * lat/long decimals to the closest multiple of si.
    * So, for example, si = 0.25 would round to closest 1/4 of degree.
    * @param si Smallest increment
    * @param d Original value
    * @return new value
    */
  def roundAt(si: Double)(d: Double) = round(d / si) * si

  /**
    * This function returns new point where the latitude and longitude
    * degrees have been rounded up to given decimals. In effect, this creates
    * discretized grid to which all locations are mapped to.
    * This is deterministic - same location will always end up with the same new value.
    *
    * @param decimals Number of decimals for the new accuracy of the location.
    *                 Same value is used for latitude and longitude.
    * @param p The original point to be rounded
    * @return The new location Point
    */
  def discretize(decimals: Int)(p: Point): Point = {
    val transformLat = roundAt(decimals) _
    val transformLong = roundAt(decimals) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  // random generators
  val randGen = new scala.util.Random
  def randomUniform(min: Double, max: Double): Double = min + (max - min) * randGen.nextDouble
  def randomNormal(mean: Double, std: Double): Double = mean + std * randGen.nextGaussian
  def randomTruncatedNormal(mean: Double, std: Double): Double = {
    @scala.annotation.tailrec
    def check(g: Double): Double = {
      if (abs(g) <= 2.0) g // max of 2 x std allowed
      else check(randGen.nextGaussian)
    }
    val n = check(randGen.nextGaussian)
    mean + std * n
  }


  /**
    * Transfer function that adds uniform noise to the value.
    *
    * @param maxDiff Max positive and negative difference that can happen (in degrees)
    * @param d Original value
    * @return New value
    */
  def transformUniformNoise(maxDiff: Double)(d: Double): Double =
    d + randomUniform(-maxDiff, maxDiff)

  /**
    * This function returns a new point where the location has been moved by
    * adding uniform noise to it.
    * This function is nondeterministic, different value each time.
    *
    * @param noiseLatMax
    * @param noiseLongMax
    * @param p Original point
    * @return New point
    */
  def additiveUniformNoise(noiseLatMax: Double, noiseLongMax: Double)(p: Point) = {
    val transformLat = transformUniformNoise(noiseLatMax) _
    val transformLong = transformUniformNoise(noiseLongMax) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  /**
    * This function returns a new point where the location has been moved by
    * adding uniform noise to it.
    * This function is nondeterministic, different value each time.
    *
    * @param noiseLatMaxMeters
    * @param noiseLongMaxMeters
    * @param p Original point
    * @return New point
    */
  def additiveUniformNoiseMeters(noiseLatMaxMeters: Double, noiseLongMaxMeters: Double)(p: Point) = {
    val transformLat = transformUniformNoise(latitudeOffsetInDeg(noiseLatMaxMeters)) _
    val transformLong = transformUniformNoise(longitudeOffsetInDeg(p.latitude, noiseLongMaxMeters)) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  /**
    * Transfer function that adds Gaussian noise to the value.
    *
    * @param std Standard deviation of the Gaussian noise to be added
    * @param d Original value
    * @return New value
    */
  def transformGaussianNoiseGen(std: Double)(d: Double): Double =
    d + randomTruncatedNormal(0.0, std)

  /**
    * This function returns new point so that the location has been moved by
    * adding Gaussian noise to it. The parameters are shared for latitude and longitude.
    * This function is nondeterministic, different value each time.
    *
    * @param std Standard deviation of the Gaussian noise to be added
    * @param p Original point
    * @return New point
    */
  def additiveGaussianNoise(std: Double)(p: Point) = {
    val transformLat = transformGaussianNoiseGen(std) _
    val transformLong = transformGaussianNoiseGen(std) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  /**
    * This function returns new point so that the location has been moved by
    * adding Gaussian noise to it. The parameters are shared for latitude and longitude.
    * This function is nondeterministic, different value each time.
    *
    * @param stdMeters Standard deviation of the Gaussian noise to be added
    * @param p Original point
    * @return New point
    */
  def additiveGaussianNoiseMeters(stdMeters: Double)(p: Point) = {
    val stdLat = latitudeOffsetInDeg(stdMeters)
    val stdLong = longitudeOffsetInDeg(p.latitude, stdMeters)
    val transformLat = transformGaussianNoiseGen(stdLat) _
    val transformLong = transformGaussianNoiseGen(stdLong) _
    pointTransformerGen(transformLat, transformLong)(p)
  }

  /**
    * Returns the midpoint of the bounding geohash. Another way to discretize the location.
    * @param hashLength length of the geohash determining the size of the bounding box
    * @param p Original point
    * @return New point
    */
  def discretizeWithGeoHash(hashLength: Int)(p: Point) =
    GeoHash.decode(p.geoHash(hashLength))

}
