package game.components.gameplay

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import doppelengine.component.Component
import game.components.gameplay.physics.DimensionComponent
import doppelengine.entity.EntityId
import game.components.common.io.ClientCommand

object GamePlayObserverComponent {
  val props = Props(classOf[GamePlayObserverComponent])

  // received
  case class UpdateEntities(snaps: Set[(EntityId, DimensionComponent.Snapshot)])

}

class GamePlayObserverComponent extends Component {

  import GamePlayObserverComponent._

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
}