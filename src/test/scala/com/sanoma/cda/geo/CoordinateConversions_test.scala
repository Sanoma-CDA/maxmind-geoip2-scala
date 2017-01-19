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

import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import org.scalatest.Matchers._

class CoordinateConversions_test extends FunSuite with PropertyChecks {
  import CoordinateConversions._
  import math.{sqrt, pow}

  // http://coordtrans.fgi.fi/transform-form.do
  // From:
  // Datum: ETRS89
  // Coordinate reference system: EUREF-FIN (Geographic 2D)
  // Height system: None
  // Projection zone:
  //
  // To:
  // Datum: ETRS89
  // Coordinate reference system: ETRS-TM35FIN (Projected)
  // Height system: None
  // Projection zone: ETRS-TM35FIN

  val points2test = List(
    ((60.1672065,24.943796), (6671809.1658, 385901.3110)),
    ((60.1665885,24.9671708), (6671700.2017, 387195.9828)),
    ((60.6642356,27.8847703), (6725714.8244, 548356.8170)),
    ((60.4357173,22.1715417), (6709680.8713, 234390.6579)),
    ((60.2242436,19.5317543), (6699805.5509, 86856.5695)),
    ((62.8824174,27.6986303), (6972681.4182, 535529.1509)),
    ((65.8223582,24.1582863), (7303047.9565, 370163.2372)),
    ((66.1673922,29.1381403), (7340213.6946, 596391.2855)),
    ((67.649597,24.9129173), (7505290.2243, 411446.0674)),
    ((62.9691274,22.9939003), (6988475.8600, 296963.7325))
  )

  def dist(t1: (Double, Double), t2: (Double, Double)) = sqrt(pow(t1._1 - t2._1, 2) + pow(t1._1 - t2._1, 2))

  test("wgs842etrs89tm35fin") {
    for (pair <- points2test) {
      val p = pair._1
      val NE = pair._2
      val ne = wgs842etrs89tm35fin(p)
      dist(NE, ne) should be <= 1.0 // less than 1 meter
    }
  }

  test("etrs89tm35fin2wgs84") {
    for (pair <- points2test) {
      val P = pair._1
      val NE = pair._2
      val p = etrs89tm35fin2wgs84(NE)
      funcs.distanceHaversine(P, p) should be <= 1.0 // less than 1 meter
    }
  }

}
