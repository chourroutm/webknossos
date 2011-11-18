package com.scalableminds.tools.geometry

import scala.math._
/**
 * scalableminds - brainflight
 * User: tmbo
 * Date: 17.11.11
 * Time: 20:52
 */

/**
 * Representation of a regular polygon in a 2 dimensional space
 * n - is the number of vertices and a is the apothem
 * http://en.wikipedia.org/wiki/Regular_polygon
 */
class RegularPolygon(n: Int, a: Int) {
  val vertices = {
    var vertices = List[Vector2D]();
    // angle between two diagonals
    val alpha = 2*Pi/n
    // vector to the first vertex
    var pointVector = new Vector2D(-tan(alpha/2)*a,a)
    vertices ::= pointVector
    // length of the polygons sides
    val s=2*tan(alpha/2)*a
    // vector which is used to generate the following vertices
    // through iteration, rotating and vector addition
    var v = new Vector2D(-s, 0)
    for(i <- 1 until n){
      v = v.rotate(alpha)
      pointVector = pointVector + v
      vertices = vertices ::: pointVector :: Nil
    }
    vertices
  }
  /**
   * Returns the vertex at the given index. Negative indices are used to navigate clockwise
   */
  def vertex(index: Int) = {
    val i = (index%vertices.size)
    if(i<0) vertices(vertices.size+i) else vertices(i)
  }
  // convert polygon to a valid json representation
  def to3D(value: Int, position: Int) = vertices.map(v => v.to3D(value,position))
}

/**
 * Simple polygon implementation
 */
class Polygon(vertices: List[Vector3D]){
  override def toString = vertices.mkString("[",",","]")
}
