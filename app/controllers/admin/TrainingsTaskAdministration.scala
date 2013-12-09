package controllers.admin

import play.mvc.Security.Authenticated
import oxalis.security.Secured
import models.security._
import play.api.data.Form
import play.api.data.Forms._
import oxalis.security.AuthenticatedRequest
import models.task._
import braingames.binary.models.DataSet
import models.user.{ExperienceService, Experience}
import oxalis.nml._
import play.api.i18n.Messages
import java.util.Date
import models.annotation.{AnnotationService, AnnotationType, AnnotationDAO}
import models.tracing.skeleton.SkeletonTracing
import views._
import controllers.Controller
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.Some
import org.joda.time.DateTime

object TrainingsTaskAdministration extends AdminController {

  override val DefaultAccessPermission = Some(Permission("admin.review", List("access")))

  val trainingsTaskForm = Form(
    tuple(
      "task" -> text,
      "tracing" -> text.verifying("tracing.notFound", exp => SkeletonTracing.findOneById(exp).isDefined),
      "training" -> mapping(
        "domain" -> nonEmptyText(1, 50),
        "gain" -> number,
        "loss" -> number)(Training.fromForm)(Training.toForm)))
                          .fill("", "", Training.empty)

  def taskToForm(t: Task) = {
    (t.id, "", Training.empty)
  }

  def list = Authenticated().async { implicit request =>
    for {
      trainings <- TaskDAO.findAllTrainings
    } yield {
      Ok(html.admin.training.trainingsTaskList(trainings))
    }
  }

  def trainingsTaskCreateHTML(taskForm: Form[(String, String, Training)])(implicit request: AuthenticatedRequest[_]) = {
    for {
      nonTrainings <- TaskService.findAllNonTrainings
      experiences <- ExperienceService.findAllDomains
      annotations <- AnnotationService.openExplorationalFor(request.user).collect[List]()
    } yield {
      html.admin.training.trainingsTaskCreate(
        nonTrainings,
        annotations,
        experiences.toList,
        taskForm)
    }
  }

  def create(taskId: String) = Authenticated().async { implicit request =>
    for {
      taskOpt <- TaskDAO.findOneById(taskId)
      form = taskOpt.map(task => trainingsTaskForm.fill(taskToForm(task))) getOrElse trainingsTaskForm
      html <- trainingsTaskCreateHTML(form)
    } yield {
      Ok(html)
    }
  }

  def createFromForm = Authenticated().async(parse.multipartFormData) { implicit request =>
    trainingsTaskForm.bindFromRequest.fold(
      hasErrors = (formWithErrors => trainingsTaskCreateHTML(formWithErrors).map(html => BadRequest(html))),
      success = {
        case (taskId, annotationId, training) =>
          for {
            task <- TaskDAO.findOneById(taskId) ?~> Messages("task.notFound")
            annotation <- AnnotationDAO.findOneById(annotationId) ?~> Messages("annotation.notFound")
            trainingsTask <- TaskService.copyDeepAndInsert(task.copy(
              instances = Integer.MAX_VALUE,
              created = DateTime.now()),
              includeUserTracings = false).toFox
            sample <- AnnotationService.createSample(annotation, trainingsTask._id).toFox
            _ <- TaskService.setTraining(trainingsTask, training, sample)
            trainings <- TaskDAO.findAllTrainings
          } yield {
            Ok(html.admin.training.trainingsTaskList(trainings))
          }
      })
  }

  def delete(taskId: String) = Authenticated().async { implicit request =>
    for {
      task <- TaskDAO.findOneById(taskId) ?~> Messages("task.training.notFound")
      _ <- TaskService.remove(task._id)
    } yield {
      JsonOk(Messages("task.training.deleted"))
    }
  }
}