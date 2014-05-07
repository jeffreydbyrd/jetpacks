package game.components.io

import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import doppelengine.component.Component
import game.components.physics.DimensionComponent
import doppelengine.entity.EntityId
import game.components.startscreen.ReadyComponent

object ObserverComponent {
  val props = Props(classOf[ObserverComponent])

  // received
  case class UpdateEntities(snaps: Set[(EntityId, DimensionComponent.Snapshot)])

  case class UpdateReadyMenu(snaps: Set[(EntityId, ReadyComponent.Snapshot)])

}

class ObserverComponent extends Component {

  import ObserverComponent._

  var connection: Option[ActorRef] = None

  // All the things that I think are in the room with me:
  var snapshots: Map[EntityId, DimensionComponent.Snapshot] = Map()

  override def receive = LoggingReceive {
    case UpdateEntities(snaps) if connection.isDefined =>
      val Some(conn) = connection
      for ((id, snap) <- snaps if !snapshots.contains(id))
        conn ! ClientCommand.CreateRect(id.toString, snap.pos, snap.shape)

      val movements =
        for {
          (id, snap) <- snaps
          if snapshots.contains(id) && snap.pos != snapshots(id).pos
        } yield id.toString ->(snap.pos.x, snap.pos.y)

      if (movements.nonEmpty)
        conn ! ClientCommand.UpdatePositions(movements.toMap)

      snapshots = snaps.toMap

    case conn: ActorRef => connection = Some(conn)
  }

  override def postStop() = {
    for (c <- connection) c ! PoisonPill
  }
}