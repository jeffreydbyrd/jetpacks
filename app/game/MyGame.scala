package game

import akka.actor.{ActorRef, ActorSystem}

import doppelengine.core.Engine
import game.systems.{ReadySystem, VisualSystem, QuitSystem}
import game.systems.physics.PhysicsSystem
import scala.concurrent.duration._
import game.systems.connection.ConnectionSystem
import scala.concurrent.{Await, Future}
import doppelengine.system.SystemConfig

object MyGame {

  val numPlayers = 2

  private val sysConfigs: Set[SystemConfig] = Set(
    SystemConfig(ConnectionSystem.props, "connection-system"),
    SystemConfig(QuitSystem.props, "quit-system"),
    SystemConfig(ReadySystem.props, "ready-system")
  )

  implicit val timeout: akka.util.Timeout = 1.second

  private val actorSystem: ActorSystem = akka.actor.ActorSystem("Doppelsystem")

  private val doppelengine: ActorRef =
    actorSystem.actorOf(Engine.props(sysConfigs, Set()), name = "engine")

  val connectionSystem = {
    val fConnSystem: Future[ActorRef] =
      actorSystem.actorSelection(doppelengine.path / "connection-system").resolveOne
    Await.result(fConnSystem, 1000 millis)
  }
}
