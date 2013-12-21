import akka.actor.Props
import braingames.reactivemongo.GlobalDBAccess
import models.annotation.AnnotationDAO
import models.security.Permission
import models.task.TimeSpan
import models.team._
import models.team.TeamTree
import models.user.time.TimeEntry
import oxalis.thirdparty.BrainTracing
import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.Play.current
import models.security._
import models.task._
import models.user._
import braingames.image.Color
import models.task._
import models.binary._
import models.security.Role
import models.tracing._
import models.basics.{BasicEvolution}
import oxalis.mail.DefaultMails
import braingames.geometry._
import oxalis.annotation.{AnnotationStore}
import braingames.mail.Mailer
import scala.collection.parallel.Tasks
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import scala.Some
import scala.util._
import oxalis.binary.BinaryDataService
import com.typesafe.config.Config

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    val conf = Play.current.configuration

    startActors(conf.underlying)

    if (conf.getBoolean("application.insertInitialData") getOrElse false) {
      InitialData.insertRoles()
      InitialData.insertUsers()
      InitialData.insertTaskAlgorithms()
      InitialData.insertTeams()
    }

    BinaryDataService.start(onComplete = {
      if (Play.current.mode == Mode.Dev) {
        BasicEvolution.runDBEvolution()
        // Data insertion needs to be delayed, because the dataSets need to be
        // found by the DirectoryWatcher first
        InitialData.insertTasks()
      }
      Logger.info("Directory start completed")
    })

    Role.ensureImportantRoles()
  }

  override def onStop(app: Application) {
    BinaryDataService.stop()
    models.context.db.close()
  }

  def startActors(conf: Config) {
    Akka.system.actorOf(
      Props(new AnnotationStore()),
      name = "annotationStore")
    Akka.system.actorOf(Props(new Mailer(conf)), name = "mailActor")
  }
}

/**
 * Initial set of data to be imported
 * in the sample application.
 */
object InitialData extends GlobalDBAccess {

  def insertRoles() = {
    if (Role.findAll.isEmpty) {
      Role.insertOne(Role("user", Nil, Color(0.2274F, 0.5294F, 0.6784F, 1)))
      Role.insertOne(Role("admin", Permission("admin.*", "*" :: Nil) :: Nil, Color(0.2F, 0.2F, 0.2F, 1)))
      Role.insertOne(Role("reviewer",
        Permission("admin.review.*", "*" :: Nil) ::
          Permission("admin.menu", "*" :: Nil) :: Nil,
        Color(0.2745F, 0.5333F, 0.2784F, 1)))
    }
  }

  def insertUsers() = {
    if (User.findOneByEmail("scmboy@scalableminds.com").isEmpty) {
      println("inserted")
      User.insertOne(User(
        "scmboy@scalableminds.com",
        "SCM",
        "Boy",
        true,
        braingames.security.SCrypt.hashPassword("secret"),
        List(TeamMembership(
          TeamPath("Structure of Neocortical Circuits Group" :: Nil),
          TeamMembership.Admin)),
        "local",
        UserConfiguration.defaultConfiguration,
        Set("user", "admin")))
    }
  }

  def insertTaskAlgorithms() = {
    if (TaskSelectionAlgorithm.findAll.isEmpty) {
      TaskSelectionAlgorithm.insertOne(TaskSelectionAlgorithm(
        """function simple(user, tasks){ 
          |  return tasks[0];
          |}""".stripMargin))
    }
  }

  def insertTeams() = {
    TeamTreeDAO.findOne.map {
      case Some(_) =>
      case _ =>
        TeamTreeDAO.insert(TeamTree(Team("Structure of Neocortical Circuits Group", Nil)))
    }
  }

  def insertTasks() = {
    if (TaskType.findAll.isEmpty) {
      val tt = TaskType(
        "ek_0563_BipolarCells",
        "Check those cells out!",
        TimeSpan(5, 10, 15))
      TaskType.insertOne(tt)
    }
  }
}
