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

import scala.xml.Elem

object KML {

  type KMLPlacemark = Elem
  type KMLFolder = Elem
  type KMLColor = String

  /**
   * Write this out like this
   * import scala.xml.XML
   * val out = new java.io.PrintWriter("my.kml")
   * XML.write(out, kml, "utf-8", xmlDecl = true, doctype = null)
   * out.close
   */
  def getKMLMasterDoc(folders: Seq[KMLFolder], documentID: String = "Geo areas") =
    <kml xmlns="http://www.opengis.net/kml/2.2">
      <Document id={documentID}>
        <open>1</open>
      { folders }
      </Document>
    </kml>


  def getKMLFolder(name: String, folders: List[KMLFolder]): KMLFolder =
    <Folder id={name}>
      <name>{name}</name>
      <open>0</open>
      <visibility>1</visibility>
      { folders }
    </Folder>

  def getKMLFolder(name: String, placemarks: Seq[KMLPlacemark]): KMLFolder =
    <Folder id={name}>
      <name>{name}</name>
      <open>0</open>
      <visibility>1</visibility>
      { placemarks }
    </Folder>

  def rgb2KMLColor(rgb: (Double, Double, Double)) = rgba2KMLColor((rgb._1, rgb._2, rgb._3, 1.0))
  // KML is strange: hexBinary value: aabbggrr
  def rgba2KMLColor(rgba: (Double, Double, Double, Double)): KMLColor = {
    val (r, g, b, a) = rgba
    require(0.0 <= r && r <= 1.0, "It is required that 0.0 <= r <= 1.0")
    require(0.0 <= g && g <= 1.0, "It is required that 0.0 <= g <= 1.0")
    require(0.0 <= b && b <= 1.0, "It is required that 0.0 <= b <= 1.0")
    require(0.0 <= a && a <= 1.0, "It is required that 0.0 <= a <= 1.0")

    def scale(from: (Double, Double), to: (Double, Double))(value: Double): Double = {
      // these are evaluated every time... slightly slower obviously
      //require(from._2 > from._1, "'from' range max must be larger than min")
      //require(to._2 > to._1, "'to' range max must be larger than min")
      val orig_min = from._1
      val orig_range = from._2 - from._1
      to._1 + (to._2 - to._1) * ((value - orig_min) / orig_range)
    }
    def scaler(v: Double): Int = math.round(scale((0.0, 1.0), (0, 255))(v)).toInt
    def toHex(i: Int): String = if (i <= 15) "0" + i.toHexString else i.toHexString
    (toHex(scaler(a)) + toHex(scaler(b)) + toHex(scaler(g)) + toHex(scaler(r))).toUpperCase
  }

  def point2KMLString(p: Point, height: Double) = p.longitude + "," + p.latitude + "," + height


  def geoArea2KMLPlacemark(geo: GeoArea, name: String, height: Double = 0.0, lineColorHex: Option[KMLColor], polyColorHex: Option[KMLColor]): KMLPlacemark = {
    geo match {
      case poly: Polygon => polygon2KMLPlacemark(poly, name, height, lineColorHex, polyColorHex)
      case rect: Rectangle => polygon2KMLPlacemark(Rectangle.rectangle2Polygon(rect), name, height, lineColorHex, polyColorHex)
      case circle: Circle => polygon2KMLPlacemark(Circle.circle2Polygon(circle, 60), name, height, lineColorHex, polyColorHex)
      case _ => unknownPlacemark
    }
  }

  def polygon2KMLPlacemark(poly: Polygon, name: String, height: Double = 0.0, lineColorHex: Option[KMLColor], polyColorHex: Option[KMLColor]): KMLPlacemark = {
    <Placemark>
      <name>{name}</name>
      <visibility>1</visibility>
      {
      if (lineColorHex.isDefined || polyColorHex.isDefined){
        <Style>
          {
          lineColorHex.map{c => <LineStyle><color>{c.toUpperCase}</color></LineStyle>} ++
            polyColorHex.map{c => <PolyStyle><color>{c.toUpperCase}</color></PolyStyle>}
          }
        </Style>
      }
      }
      <Polygon id={"P_" + name}>
        <extrude>1</extrude>
        { if (height == 0.0) <altitudeMode>clampToGround</altitudeMode>
        else <altitudeMode>relativeToGround</altitudeMode> }
        <outerBoundaryIs>
          <LinearRing id={"LR_" + name}>
            <coordinates>
              { poly.points.map(point2KMLString(_, height)).mkString(" ") }
            </coordinates>
          </LinearRing>
        </outerBoundaryIs>
      </Polygon>
    </Placemark>
  }

  def unknownPlacemark: KMLPlacemark = <Placemark></Placemark>
}
