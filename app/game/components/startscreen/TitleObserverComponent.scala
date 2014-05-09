package game.components.startscreen

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import doppelengine.entity.EntityId
import game.components.startscreen.ReadyComponent.Snapshot
import game.components.io.ClientCommand

object TitleObserverComponent {
  def props = Props[TitleObserverComponent]

  // received
  case class UpdateEntities(snaps: Set[(EntityId, ReadyComponent.Snapshot)])

}

class TitleObserverComponent extends Actor {

  import TitleObserverComponent.UpdateEntities

  var connection: Option[ActorRef] = None

  override def receive: Receive = {
    case conn: ActorRef => connection = Some(conn)
    case UpdateEntities(snaps) if connection.isDefined =>
      val Some(conn) = connection

      val tuples =
        for ((id, Snapshot(isReady)) <- snaps) yield {
          val str = id.toString
          str.substring(str.lastIndexOf('/')) -> isReady
        }

      conn ! ClientCommand.UpdateIntro(tuples.toMap)
  }

  override def postStop() = {
    for (c <- connection) c ! PoisonPill
  }
}
