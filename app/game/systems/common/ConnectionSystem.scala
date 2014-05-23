package game.systems.common

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import scala.concurrent.duration._
import play.api.libs.iteratee.Enumerator
import doppelengine.system.System
import akka.actor.Terminated
import doppelengine.entity.{Entity, EntityConfig}
import game.components.common.connection.PlayActorConnection
import play.api.libs.json.{Json, JsValue}
import game.MyGame
import game.entities.startscreen.StartScreenEntity
import akka.util.Timeout

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

  implicit val timeout: Timeout = 1.second

  var connections: Map[String, ActorRef] = Map()
  var numConnections: Int = 0

  def connectPlayer(username: String) = {
    val (enumerator, channel) = play.api.libs.iteratee.Concurrent.broadcast[JsValue]

    val config: EntityConfig = StartScreenEntity.config(username)
    val connection =
      context.actorOf(PlayActorConnection.props(channel), s"conn-$username")

    createEntities(Set(config)).foreach(_ => {
      val inputSel = context.actorSelection(s"../input-$username")
      val observerSel = context.actorSelection(s"../observer-$username")

      for (ref <- inputSel.resolveOne) connection ! ref
      for (ref <- observerSel.resolveOne) ref ! connection
    })

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
