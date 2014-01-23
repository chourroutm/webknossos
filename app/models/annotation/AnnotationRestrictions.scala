package models.annotation

import models.user.User
import models.security.{RoleDAO, Role}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.async.Async._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import braingames.reactivemongo.GlobalAccessContext

/**
 * Company: scalableminds
 * User: tmbo
 * Date: 02.06.13
 * Time: 02:02
 */

class AnnotationRestrictions {
  def allowAccess(user: Option[User]): Boolean = false

  def allowUpdate(user: Option[User]): Boolean = false

  def allowFinish(user: Option[User]): Boolean = false

  def allowDownload(user: Option[User]): Future[Boolean] = Future.successful(false)

  def allowAccess(user: User): Boolean = allowAccess(Some(user))

  def allowUpdate(user: User): Boolean = allowUpdate(Some(user))

  def allowFinish(user: User): Boolean = allowFinish(Some(user))

  def allowDownload(user: User): Future[Boolean] = allowDownload(Some(user))

}

object AnnotationRestrictions {
  def writeAsJson(ar: AnnotationRestrictions, u: Option[User]): Future[JsObject] =
    for {
      isDownloadAllowed <- ar.allowDownload(u)
    } yield {
      Json.obj(
        "allowAccess" -> ar.allowAccess(u),
        "allowUpdate" -> ar.allowUpdate(u),
        "allowFinish" -> ar.allowFinish(u),
        "allowDownload" -> isDownloadAllowed
      )
    }

  def restrictEverything =
    new AnnotationRestrictions()

  def defaultAnnotationRestrictions(annotation: AnnotationLike) =
    new AnnotationRestrictions {
      override def allowAccess(user: Option[User]) = {
        user.map {
          user =>
            annotation._user == user._id || (RoleDAO.Admin.map(user.hasRole) getOrElse false)
        } getOrElse false
      }

      override def allowUpdate(user: Option[User]) = {
        user.map {
          user =>
            annotation._user == user._id && !annotation.state.isFinished
        } getOrElse false
      }

      override def allowFinish(user: Option[User]) = {
        user.map {
          user =>
            (annotation._user == user._id || (RoleDAO.Admin.map(user.hasRole) getOrElse false)) && !annotation.state.isFinished
        } getOrElse false
      }

      override def allowDownload(user: Option[User]) = async {
        user.map {
          user =>
            !annotation.isTrainingsAnnotation && allowAccess(user)
        } getOrElse false
      }
    }
}
