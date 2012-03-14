package controllers

import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json._
import models.{ TrackedRoute, RouteOrigin }
import play.api.mvc._
import org.bson.types.ObjectId
import brainflight.tools.Math._
import brainflight.security.Secured
import brainflight.tools.geometry.Vector3I
import brainflight.tools.geometry.Vector3I._
import brainflight.tools.ExtendedTypes._
import models.{ User, TransformationMatrix }
import models.Role

/**
 * scalableminds - brainflight
 * User: tmbo
 * Date: 19.12.11
 * Time: 11:27
 */
object Route extends Controller with Secured {
  override val DefaultAccessRole = Role( "user" )

  val PointValue = 0f
  val BranchPushVallue = 1f
  val BranchPopValue = 2f
  
  def initialize = Authenticated() { user =>
    implicit request =>
      val originOption = (user.useBranchPointAsOrigin) orElse (RouteOrigin.useLeastUsed)
      ( for {
        origin <- originOption
        startPoint <- origin.extractTranslation
      } yield {
        val route = TrackedRoute.createForUser(
          user,
          startPoint.toVector3I :: Nil )

        val data = Map(
          "id" -> toJson( route._id.toString ),
          "matrix" -> toJson( origin.value ), 
          "branches" -> toJson ( user.branchPoints.map( _.value).reverse )
        )
        
        Ok( toJson( data ) )
      } ) getOrElse NotFound( "Couldn't open new route." )
  }
  /**
   *
   */
  def blackBox( id: String ) = Action[RawBuffer]( parse.raw( 1024 * 1024 ) ) {
    implicit request =>

      ( for {
        user <- maybeUser
        route <- TrackedRoute.findOpenBy( id )
        buffer <- request.body.asBytes( 1024 * 1024 )
        if ( route.userId == user._id )
      } yield {
        var points = Vector.empty[Vector3I]
        
        val floatBuffer = buffer.subDivide( 4 ).map( _.reverse.toFloat )
        Logger.debug("Route received")
        floatBuffer.dynamicSliding( windowSize = 17 ) {
          case PointValue :: x :: y :: z :: _ =>
            val v = Vector3I( x.toInt, y.toInt, z.toInt )
            points = points :+ v
            
            Vector3I.defaultSize
          case BranchPushVallue :: tail =>
            val matrix = tail.take(16)
            Logger.debug("PUSH branchpoint: "+matrix)
            route.add( points.toList )
            points = Vector.empty
            User.save( user.copy( branchPoints = TransformationMatrix( matrix ) :: user.branchPoints ) )
            route.addBranch()
            
            TransformationMatrix.defaultSize
          case BranchPopValue :: _ =>
            Logger.debug("POP branchpoint")
            route.add( points.toList )
            points = Vector.empty
            if ( !user.branchPoints.isEmpty ) {
              val branchPoint = user.branchPoints.head
              User.save( user.copy( branchPoints = user.branchPoints.tail ) )
              route.closeBranch( branchPoint.extractTranslation.get.toVector3I )
            }
            
            0
          case _ =>
            Logger.error("Recieved control code is invalid.")     
            floatBuffer.size // jump right to the end to stop processing
        }
        route.add( points.toList )
        Ok
      } ) getOrElse BadRequest( "No open route found or byte array invalid." )

  }
  def getRoute( id: String ) = Authenticated() { user =>
    implicit request =>
      TrackedRoute.findOneByID( new ObjectId( id ) ).map( route =>
        Ok( toJson( route.points ) )
      ) getOrElse NotFound( "Couldn't open route." )
  }
}