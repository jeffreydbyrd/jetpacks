package game.systems.common.connection

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.iteratee.Enumerator
import game.components.gameplay.io.InputComponent
import doppelengine.component.ComponentConfig
import doppelengine.system.System
import akka.actor.Terminated
import doppelengine.entity.{EntityId, Entity, EntityConfig}
import game.components.gameplay.io.connection.PlayActorConnection
import game.components.types._
import play.api.libs.json.{Json, JsValue}
import game.MyGame
import game.components.startscreen.{ReadyComponent, TitleObserverComponent}

object ConnectionSystem {
  def props = Props(classOf[ConnectionSystem])

  private def err(msg: String): NotConnected = {
    NotConnected(
      Json.obj(
        "seq" -> 0,
        "ack" -> false,
        "type" -> "error",
        "message" -> msg
      )
    )
  }

  // received
  case class AddPlayer(name: String)

  // sent
  case object Connect

  case class Connected(connection: ActorRef, enum: Enumerator[JsValue])

  case class NotConnected(message: JsValue)

}

class ConnectionSystem extends System(0.millis) {

  import ConnectionSystem._

  var connections: Map[String, ActorRef] = Map()
  var numConnections: Int = 0

  def connectPlayer(username: String) = {
    val (enumerator, channel) = play.api.libs.iteratee.Concurrent.broadcast[JsValue]

    val connection =
      context.actorOf(PlayActorConnection.props(channel), s"conn-$numConnections")

    val input =
      new ComponentConfig(InputComponent.props, s"input-$numConnections")
    val observer =
      new ComponentConfig(TitleObserverComponent.props, s"observer-$numConnections")
    val ready =
      new ComponentConfig(ReadyComponent.props, s"ready-$numConnections")

    val id = (context.parent.path / username).toString

    val config: EntityConfig = EntityConfig(
      EntityId(id, username),
      Map(
        Input -> input, TitleObserver -> observer, Ready -> ready
      )
    )

    context.actorOf(
      Helper.props(context.parent, connection, numConnections, this.version, config),
      s"helper-$numConnections")

    sender ! Connected(connection, enumerator)
    numConnections += 1
    connections += username -> connection
    context.watch(connection)
  }

  override def updateEntities(entities: Set[Entity]): Unit = {}

  override def onTick(): Unit = {}

  override def receive: Receive =
    super.receive orElse {
      case AddPlayer(username) =>
        if (connections.contains(username))
          sender ! err(s"username '$username' already in use")
        else if (connections.size == MyGame.numPlayers)
          sender ! err("Max players already reached")
        else
          connectPlayer(username)

      case Terminated(conn) =>
        connections = connections.filterNot {
          case (usrName, actRef) => actRef == conn
        }
    }
}
