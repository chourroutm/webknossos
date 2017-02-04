/*
 * Copyright (C) 20011-2014 Scalable minds UG (haftungsbeschränkt) & Co. KG. <http://scm.io>
 */
package com.scalableminds.braingames.binary.models

import java.nio.file.{Paths, Path}
import com.scalableminds.braingames.binary.repository.KnossosDataSourceType

import com.scalableminds.util.geometry.{BoundingBox, Scale, Point3D}
import play.api.libs.json._

case class DataSourceSettings(
  id: Option[String],
  scale: Scale,
  priority: Option[Int])

case class DataSource(
  id: String,
  baseDir: String,
  scale: Scale,
  priority: Int = 0,
  dataLayers: List[DataLayer] = Nil,
  sourceType: Option[String] = Some(KnossosDataSourceType.name),
  blockLengthOpt: Option[Int] = Some(128)
) {

  //  lazy val sourceFolder: Path =
  //    Paths.get(baseDir)

  /**
    * Number of voxels per dimension in the storage format
    */
  val cubeLength: Int =
    blockLengthOpt.getOrElse(128)

  /**
    * Defines the size of the buckets loaded from files. This is the minimal size that can be loaded from a file.
    */
  val lengthOfLoadedBuckets: Int = 32

  /**
    * Boundary of the data source
    */
  lazy val boundingBox: BoundingBox =
    BoundingBox.combine(dataLayers.map(_.boundingBox))

  def getDataLayer(name: String): Option[DataLayer] =
    dataLayers.find(_.name == name)

  def getByCategory(category: String): Option[DataLayer] =
    dataLayers.find(_.category == category)

  def relativeBaseDir(binaryBase: String): String =
    baseDir.replace(binaryBase, "")

  def pointToCube(point: Point3D, resolution: Int): Point3D =
    Point3D(
      point.x / cubeLength / resolution,
      point.y / cubeLength / resolution,
      point.z / cubeLength / resolution)

  def pointToBucket(point: Point3D, resolution: Int): Point3D =
    Point3D(
      point.x / lengthOfLoadedBuckets / resolution,
      point.y / lengthOfLoadedBuckets / resolution,
      point.z / cubeLength / resolution)

  def applyResolution(point: Point3D, resolution: Int): Point3D =
    Point3D(
      point.x / resolution,
      point.y / resolution,
      point.z / resolution)

  def unapplyResolution(point: Point3D, resolution: Int): Point3D =
    Point3D(
      point.x * resolution,
      point.y * resolution,
      point.z * resolution)

  override def toString: String =
    s"""$id (${dataLayers.map(_.name).mkString(", ")})"""
}

object DataSource {
  implicit val dataSourceFormat: Format[DataSource] =
    Json.format[DataSource]
}

object DataSourceSettings extends SettingsFile[DataSourceSettings] {

  implicit val dataSourceSettingsFormat: Format[DataSourceSettings] =
    Json.format[DataSourceSettings]

  val settingsFileName: String =
    "settings.json"

  val settingsFileReads: Format[DataSourceSettings] =
    dataSourceSettingsFormat

  def fromDataSource(dataSource: DataSource): DataSourceSettings =
    DataSourceSettings(
      Some(dataSource.id),
      dataSource.scale,
      Some(dataSource.priority)
    )

  def writeToFolder(dataSource: DataSource, path: Path): Unit = {
    val settings = fromDataSource(dataSource)
    writeSettingsToFile(settings, settingsFileInFolder(path))
  }
}