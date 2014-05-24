package game

import akka.actor.{ActorRef, ActorSystem}

import doppelengine.core.Engine
import game.systems.gameplay.physics.PhysicsSystem
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import doppelengine.system.SystemConfig
import game.systems.common.{ConnectionSystem, QuitSystem}
import game.systems.titlescreen.{StartSystem, ReadySystem}

object MyGame {

  val numPlayers = 2

  private val sysConfigs: Set[SystemConfig] = Set(
    SystemConfig(ConnectionSystem.props, "connection-system"),
    SystemConfig(QuitSystem.props, "quit-system"),
    SystemConfig(ReadySystem.props, "ready-system"),
    SystemConfig(StartSystem.props, "start-system")
  )

  implicit val timeout: akka.util.Timeout = 1.second

  private val actorSystem: ActorSystem = akka.actor.ActorSystem("Doppelsystem")

  val doppelengine: ActorRef =
    actorSystem.actorOf(Engine.props(sysConfigs, Set()), name = "engine")

  val connectionSystem = {
    val fConnSystem: Future[ActorRef] =
      actorSystem.actorSelection(doppelengine.path / "connection-system").resolveOne
    Await.result(fConnSystem, 1000 millis)
  }
}
