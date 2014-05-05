package game.systems.connection

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.iteratee.Enumerator
import game.components.io.{ObserverComponent, InputComponent}
import game.components.physics.{MobileComponent, DimensionComponent}
import doppelengine.component.ComponentConfig
import doppelengine.system.System
import akka.actor.Terminated
import doppelengine.entity.{Entity, EntityConfig}
import game.components.io.connection.PlayActorConnection
import game.components.types._
import play.api.libs.json.{Json, JsValue}
import game.MyGame

object ConnectionSystem {
  def props = Props(classOf[ConnectionSystem])

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
      new ComponentConfig(ObserverComponent.props, s"observer-$numConnections")
    val dimensions =
      new ComponentConfig(DimensionComponent.props(10, 10, 2, 2), s"dimensions-$numConnections")
    val mobility =
      new ComponentConfig(MobileComponent.props(20, 20F), s"mobile-$numConnections")

    val configs: EntityConfig = Map(
      Input -> input, Observer -> observer,
      Dimension -> dimensions, Mobility -> mobility
    )

    context.actorOf(
      Helper.props(context.parent, connection, numConnections, this.version, configs),
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
          sender ! NotConnected(Json.obj("error" -> s"username '$username' already in use"))
        else if (connections.size == MyGame.numPlayers)
          sender ! NotConnected(Json.obj("error" -> "Max players already reached"))
        else
          connectPlayer(username)

      case Terminated(conn) =>
        connections = connections.filterNot {
          case (usrName, actRef) => actRef == conn
        }
    }
}
