/*
* Copyright (C) 2011-2017 scalable minds UG (haftungsbeschränkt) & Co. KG. <http://scm.io>
*/
package com.scalableminds.braingames.datastore.controllers

import com.google.inject.Inject
import com.scalableminds.braingames.binary.helpers.DataSourceRepository
import com.scalableminds.braingames.datastore.services.{TracingContentService, WebKnossosServer}
import com.scalableminds.braingames.datastore.tracings.skeleton.{SkeletonTracingService, SkeletonUpdateAction, SkeletonUpdateActionsParser}
import com.scalableminds.braingames.datastore.tracings.volume.VolumeUpdateAction
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global

class SkeletonTracingController @Inject()(
                                         webKnossosServer: WebKnossosServer,
                                         skeletonTracingService: SkeletonTracingService,
                                         dataSourceRepository: DataSourceRepository,
                                         val messagesApi: MessagesApi,
                                         tracingRepository: TracingContentService
                                       ) extends Controller {

  def create(dataSetName: String) = Action.async {
    implicit request => {
      for {
        dataSource <- dataSourceRepository.findUsableByName(dataSetName).toFox ?~> Messages("dataSource.notFound")
      } yield {
        val tracing = skeletonTracingService.create(dataSource)
        Ok(Json.toJson(tracing))
      }
    }
  }

  def info(annotationId: String) = Action {
    implicit request => {
      val tracing = skeletonTracingService.findSkeletonTracing(annotationId)
      val tracingInfo = skeletonTracingService.info(tracing)
      Ok(tracingInfo)
    }
  }

  def update(annotationId: String) = Action {
    implicit request => {
      val tracing = skeletonTracingService.findSkeletonTracing(annotationId)
      val updateActions = SkeletonUpdateActionsParser.parseList(request.body.asJson)
      skeletonTracingService.update(tracing, updateActions)
      Ok
    }
  }

  def download(annotationId: String, version: Long) = Action {
    implicit request => {
      Ok
    }
  }

  def duplicate(annotationId: String, version: Long) = Action {
    implicit request => {
      Ok
    }
  }

}
