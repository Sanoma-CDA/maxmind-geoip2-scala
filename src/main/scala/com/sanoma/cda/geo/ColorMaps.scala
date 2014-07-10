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

object ColorMaps {

  def addAlpha(map: (Double) => (Double, Double, Double), x: Double, alpha: Double = 1.0) = {
    val color = map(x)
    (color._1, color._2, color._3, alpha)
  }

  /**
   * The Default = jet
   *
  n = max(round(m/4),1);
     x = (1:n)'/n;
     y = (n/2:n)'/n;
     e = ones(length(x),1);
     r = [0*y; 0*e; x; e; flipud(y)];
     g = [0*y; x; e; flipud(x); 0*y];
     b = [y; e; flipud(x); 0*e; 0*y];
     J = [r g b];
     while size(J,1) > m
     J(1,:) = [];
     if size(J,1) > m, J(size(J,1),:) = []; end
     end
   */
  def jet(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    if (x <= 1.0 / 8.0) {
      val low = -1.0 / 8.0
      val high = 1.0 / 8.0
      (0.0, 0.0, (x - low) / (high - low))
    }
    else if (x <= 3.0 / 8.0) {
      val low = 1.0 / 8.0
      val high = 3.0 / 8.0
      (0.0, (x - low) / (high - low), 0.0)
    }
    else if (x <= 5.0 / 8.0) {
      val low = 3.0 / 8.0
      val high = 5.0 / 8.0
      ((x - low) / (high - low), 1.0, (high - x) / (high - low))
    }
    else if (x <= 7.0 / 8.0) {
      val low = 5.0 / 8.0
      val high = 7.0 / 8.0
      (1.0, (high - x) / (high - low), 0.0)
    }
    else {
      val low = 7.0 / 8.0
      val high = 9.0 / 8.0
      ((high - x) / (high - low), 0.0, 0.0)
    }
  }

  /**
   * hot
     n = fix(3/8*m);

     r = [(1:n)'/n; ones(m-n,1)];
     g = [zeros(n,1); (1:n)'/n; ones(m-2*n,1)];
     b = [zeros(2*n,1); (1:m-2*n)'/(m-2*n)];

     h = [r g b];
   */
  def hot(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    if (x <= 3.0 / 8.0) {
      val low = 0.0
      val high = 3.0 / 8.0
      ((x - low) / (high - low), 0.0, 0.0)
    }
    else if (x <= 6.0 / 8.0) {
      val low = 3.0 / 8.0
      val high = 6.0 / 8.0
      (1.0, (x - low) / (high - low), 0.0)
    }
    else {
      val low = 6.0 / 8.0
      val high = 1.0
      (1.0, 1.0, (x - low) / (high - low))
    }
  }

  /**
   * gray, nothing really
     g = (0:m-1)'/max(m-1,1);
     g = [g g g];
   */
  def gray(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    (x, x, x)
  }

  /**
   * pink
     p = sqrt((2*gray(m) + hot(m))/3);
   */
  def pink(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    import scala.math.sqrt
    val rgbhot = hot(x)
    (sqrt(2.0 * x + rgbhot._1 / 3.0),
      sqrt(2.0 * x + rgbhot._2 / 3.0),
      sqrt(2.0 * x + rgbhot._3 / 3.0))
  }

  def cool(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    (x, 1.0 - x, 1.0)
  }

  /**
   * bone
     b = (7*gray(m) + fliplr(hot(m)))/8;
   */
  def bone(x: Double): (Double, Double, Double) = {
    require(0.0 <= x && x <= 1.0, "It is required that 0 <= x <= 1")
    val rgbhot = hot(x)
    (7.0 * x + rgbhot._3 / 8.0,
      7.0 * x + rgbhot._2 / 8.0,
      7.0 * x + rgbhot._1 / 8.0)
  }
}
