package game.systems

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import akka.pattern.ask
import doppelengine.component.Component
import game.components.io.InputComponent.Snapshot
import doppelengine.core.Engine
import doppelengine.core.Engine.Tick
import doppelengine.core.Engine.TickAck
import doppelengine.entity.Entity
import doppelengine.system.System
import akka.util.Timeout
import game.components.types.{Observer, Input}

object QuitSystem {
  implicit val timeout = Timeout(1.second)
  def props = Props( classOf[ QuitSystem ] )
}

class QuitSystem extends System {
  import QuitSystem.timeout

  val requiredComponents = List( Input, Observer )

  override def receive = manage( 0, Set() )

  def manage( version: Long, entities: Set[ Entity ] ): Receive =
    LoggingReceive {
      case System.UpdateEntities( v, ents ) if v > version =>
        val es = for ( e <- ents if e.hasComponents( requiredComponents ) )
          yield e
        context.become( manage( v, es ) )

      case Tick =>
        val setOfFutures: Set[ Future[ Entity ] ] =
          entities.map { e =>
            ( e( Input ) ? Component.RequestSnapshot )
              .mapTo[ Snapshot ]
              .filter( _.quit )
              .map( _ => e )
          }

        val futureSet: Future[ Set[ Entity ] ] = Future.sequence( setOfFutures )
        futureSet.foreach { set =>
          if ( set.nonEmpty ) context.parent ! Engine.Rem( version, set )
        }
        
        sender ! TickAck
    }
}