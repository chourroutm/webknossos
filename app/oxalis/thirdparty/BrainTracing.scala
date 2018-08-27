package oxalis.thirdparty

import com.scalableminds.util.security.SCrypt
import com.scalableminds.util.tools.{Fox, FoxImplicits}
import com.typesafe.scalalogging.LazyLogging
import models.user.User
import play.api.Play
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WS, WSAuthScheme}
import utils.WkConf

import scala.concurrent.{Future, Promise}
import scala.util._

object BrainTracing extends LazyLogging with FoxImplicits {
  val URL = "http://braintracing.org/"
  val CREATE_URL = URL + "oxalis_create_user.php"
  val LOGTIME_URL = URL + "oxalis_add_hours.php"
  val USER = "brain"
  val PW = "trace"
  val LICENSE = "hu39rxpv7m"

  lazy val Mailer =
    Akka.system(play.api.Play.current).actorSelection("/user/mailActor")

  def registerIfNeeded(user: User, password: String): Fox[String] =
    for {
      organization <- user.organization
      result <- (if (organization.name == "Connectomics department" && WkConf.Braintracing.active) register(user, password).toFox else Fox.successful("braintracing.none"))
    } yield result

  private def register(user: User, password: String): Future[String] = {
    val result = Promise[String]()
    val brainTracingRequest = WS
      .url(CREATE_URL)
      .withAuth(USER, PW, WSAuthScheme.BASIC)
      .withQueryString(
        "license" -> LICENSE,
        "firstname" -> user.firstName,
        "lastname" -> user.lastName,
        "email" -> user.email,
        "pword" -> SCrypt.md5(password))
      .get()
      .map { response =>
        result complete (response.status match {
          case 200 if isSilentFailure(response.body) =>
            Success("braintracing.error")
          case 200 =>
            Success("braintracing.new")
          case 304 =>
            Success("braintracing.exists")
          case _ =>
            Success("braintracing.error")
        })
        logger.trace(s"Creation of account ${user.email} returned Status: ${response.status} Body: ${response.body}")
      }
    brainTracingRequest.onFailure{
      case e: Exception =>
        logger.error(s"Failed to register user '${user.email}' in brain tracing db. Exception: ${e.getMessage}")
    }
    result.future
  }

  private def isSilentFailure(result: String) =
    result.contains("ist derzeit nicht verf&uuml;gbar.")

}
