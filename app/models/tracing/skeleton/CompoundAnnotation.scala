package models.tracing.skeleton

import braingames.geometry.Scale
import braingames.geometry.Point3D
import oxalis.nml._
import oxalis.nml.utils._
import models.task.{TaskDAO, TaskType, Task, Project}
import play.api.Logger
import braingames.util.TimeLogger._
import braingames.format.Formatter
import models.annotation._
import models.annotation.TemporaryAnnotation
import scala.Some
import models.annotation.Annotation
import models.annotation.AnnotationType._
import models.user.User
import org.bson.types.ObjectId
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import braingames.util.{FoxImplicits, Fox}
import braingames.reactivemongo.DBAccessContext

object CompoundAnnotation extends Formatter with FoxImplicits {

  def treePrefix(tracing: SkeletonTracingLike, user: Option[User], taskId: Option[ObjectId]) = {
    val userName = user.map(_.abreviatedName) getOrElse ""
    s"${formatHash(taskId.map(_.toString).getOrElse(""))}_${userName}_"
  }

  def renameTreesOfTracing(tracing: SkeletonTracing, user: Option[User], taskId: Option[ObjectId]) = {
    def renameTrees(prefix: String, trees: List[TreeLike]) = {
      trees.zipWithIndex.map {
        case (tree, index) =>
          tree.changeName(s"${prefix}tree%03d".format(index + 1))
      }
    }
    val temp = TemporarySkeletonTracing.createFrom(tracing, "")
    temp.copy(
      trees = renameTrees(treePrefix(tracing, user, taskId), temp.trees))
  }

  def filterAnnotation(a: Annotation) =
    a.state.isFinished

  def createFromProject(project: Project)(implicit ctx: DBAccessContext) = {
    logTime("project composition", Logger.debug) {
      for {
        tasks <- TaskDAO.findAllByProject(project.name)
        annotations <- Future.traverse(tasks)(_.annotations).map(_.flatten)
      } yield {
        createFromAnnotations(annotations, project.name, AnnotationType.CompoundProject)
      }
    }
  }

  def createFromTask(task: Task)(implicit ctx: DBAccessContext) = {
    logTime("task composition", Logger.debug) {
      for {
        annotations <- task.annotations
      } yield {
        createFromAnnotations(annotations, task.id, AnnotationType.CompoundTask)
      }
    }
  }

  def createFromTaskType(taskType: TaskType)(implicit ctx: DBAccessContext) = {
    logTime("taskType composition", Logger.debug) {
      for {
        tasks <- TaskDAO.findAllByTaskType(taskType)
        annotations <- Future.traverse(tasks)(_.annotations).map(_.flatten)
      } yield {
        createFromAnnotations(annotations, taskType.id, AnnotationType.CompoundTaskType)
      }
    }
  }

  def createFromAnnotations(annotations: List[Annotation], id: String, typ: AnnotationType)(implicit ctx: DBAccessContext): Option[TemporaryAnnotation] = {
    val as = annotations.filter(filterAnnotation)

    def annotationContent(): Fox[TemporarySkeletonTracing] = {
      val annotationsWithContent =
        Future.traverse(as)(a => a.content.map(a -> _).futureBox).map(_.flatten)

      annotationsWithContent.flatMap(Future.traverse(_) {
        case (annotation, skeleton: SkeletonTracing) =>
          annotation.user.map {
            userOpt =>
              renameTreesOfTracing(skeleton, userOpt, annotation._task.map(id => new ObjectId(id.stringify)))
          }
        case (annotation, content) =>
          Future.successful(content)
      }).map {
        tracings =>
          createFromTracings(tracings, id)
      }
    }

    as match {
      case head :: _ =>
        Some(TemporaryAnnotation(
          id,
          () => annotationContent,
          typ
        ))
      case _ =>
        None
    }
  }


  def createFromTracings(tracings: List[AnnotationContent], id: String): Option[TemporarySkeletonTracing] = {
    def mergeThem(tracings: List[AnnotationContent]): Option[TemporarySkeletonTracing] = {
      tracings match {
        case head :: tail =>
          head match {
            case t: SkeletonTracingLike =>
              val base = TemporarySkeletonTracing.createFrom(t, id)
              Some(tail.foldLeft(base) {
                case (result, tracing) =>
                  result.mergeWith(tracing)
              })
            case _ =>
              mergeThem(tail)
          }
        case _ =>
          None
      }
    }
    mergeThem(tracings.sliding(50, 50).toSeq.par.flatMap(mergeThem).toList)
  }
}