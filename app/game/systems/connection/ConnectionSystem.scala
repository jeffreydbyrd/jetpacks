package game.systems.connection

import akka.actor._
import scala.concurrent.duration._
import akka.event.LoggingReceive
import play.api.libs.iteratee.Enumerator
import game.components.io.{ObserverComponent, InputComponent}
import game.components.physics.{MobileComponent, DimensionComponent}
import doppelengine.component.ComponentConfig
import doppelengine.system.System
import doppelengine.system.System.UpdateEntities
import akka.actor.Terminated
import doppelengine.entity.{Entity, EntityConfig}
import game.components.io.connection.PlayActorConnection
import game.components.types._

object ConnectionSystem {
  def props = Props(classOf[ConnectionSystem])

  // received
  case class AddPlayer(name: String)

  // sent
  case object Connect

  case class Connected(connection: ActorRef, enum: Enumerator[String])

  case class NotConnected(message: String)

}

class ConnectionSystem extends System(0.millis) {

  import ConnectionSystem._

  var connections: Map[String, ActorRef] = Map()
  var numConnections: Int = 0

  def connectPlayer(username: String) = {
    val (enumerator, channel) = play.api.libs.iteratee.Concurrent.broadcast[String]

    val connection =
      context.actorOf(PlayActorConnection.props(channel), s"conn$numConnections")

    val input =
      new ComponentConfig(InputComponent.props, s"input_plr$numConnections")
    val observer =
      new ComponentConfig(ObserverComponent.props, s"observer_plr$numConnections")
    val dimensions =
      new ComponentConfig(DimensionComponent.props(10, 10, 2, 2), s"dimensions_plr$numConnections")
    val mobility =
      new ComponentConfig(MobileComponent.props(20, 20F), s"mobile_plr$numConnections")

    val configs: EntityConfig = Map(
      Input -> input, Observer -> observer,
      Dimension -> dimensions, Mobility -> mobility
    )

    context.actorOf(
      Helper.props(context.parent, connection, numConnections, this.version, configs),
      s"helper$numConnections")

    sender ! Connected(connection, enumerator)
    numConnections += 1
    connections += username -> connection
    context.watch(connection)
  }

  override def updateEntities(entities: Set[Entity]): Unit = {}
  override def onTick(): Unit = {}

  override def receive: Receive =
    super.receive orElse LoggingReceive {
      case AddPlayer(username) if !connections.contains(username) =>
        connectPlayer(username)

      case AddPlayer(username) =>
        sender ! NotConnected(s"username '$username' already in use")

      case Terminated(conn) =>
        connections = connections.filterNot {
          case (usrName, actRef) => actRef == conn
        }
    }
}
