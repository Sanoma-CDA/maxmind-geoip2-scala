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

// Note that the search is actually done lazily starting from the top -> make sure the polygons
// that are most expected in your data are in the beginning of the stream
class GeoAreaMap(val data: Map[String, GeoArea], searchStream: List[(String, GeoArea)], globalBB: BoundingBox) {
  def get(name: String) = data(name)
  def get(p: Point) = if (globalBB.contains(p)) getOneFromStream(p) else None
  def getAll(p: Point) = if (globalBB.contains(p)) getAllFromStream(p) else List()

  def getOneFromStream(p: Point) = searchStream.view
    .filter{ case (name, poly) => poly.mayContain(p) }
    .filter{ case (name, poly) => poly.contains(p) }
    .headOption match {
    case Some((name, poly)) => Some(name)
    case None => None
  }

  def getAllFromStream(p: Point) = searchStream.view
    .filter{ case (name, poly) => poly.mayContain(p) }
    .filter{ case (name, poly) => poly.contains(p) }.map(_._1).force
}

object GeoAreaMap {
  def fromSeq(data: Seq[(String, GeoArea)], priority: Map[String, Double] = Map()) = {
    val searchList = if (priority.isEmpty) data.toList
    else data.map{case (name, poly) => (name, poly, priority(name))}
      .toList.sortWith(_._3 > _._3)
      .map{case (name, poly, score) => (name, poly)}
    val bb = BoundingBox(data.flatMap(t => t._2.getExtremes))
    new GeoAreaMap(data.map(t => Map(t)).reduce(_ ++ _), searchList, bb)
  }

  def fromStrIter(dataStrings: Iterator[String], parse: (String => Option[(String, GeoArea)]), priority: Map[String, Double] = Map()) = {
    val data = dataStrings.map{
      line: String =>
        parse(line)
    }.flatten.toList
    fromSeq(data, priority)
  }

  def fromFile(filename: String, parse: (String => Option[(String, GeoArea)]), priority: Map[String, Double] = Map()) = {
    import scala.io.Source
    fromStrIter(Source.fromFile(filename).getLines(), parse)
  }

}

