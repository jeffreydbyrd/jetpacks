package game.systems.startscreen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import doppelengine.system.{SystemConfig, System}
import doppelengine.entity.{EntityConfig, Entity}
import game.components.types.Ready
import akka.pattern.ask
import doppelengine.component.Component.RequestSnapshot
import akka.util.Timeout
import game.components.startscreen.ReadyComponent
import scala.concurrent.{Await, Future}
import game.components.startscreen.ReadyComponent.Snapshot
import game.MyGame
import game.systems.gameplay.VisualSystem
import game.systems.gameplay.physics.PhysicsSystem
import akka.actor.{Props, ActorRef}
import game.entities.gameplay

object StartSystem {
  val props = Props[StartSystem]
}

class StartSystem extends System(500.millis) {
  implicit val timeout: Timeout = 1.second

  var starting = false
  var entities: Set[Entity] = Set()

  val fReadySystem = context.actorSelection("../ready-system").resolveOne()

  def startGame(): Unit = {
    val sysConfigs = Set(
      SystemConfig(VisualSystem.props, "visual-system"),
      SystemConfig(PhysicsSystem.props(0, -35), "physics-system")
    )

    addSystems(context.parent, sysConfigs) // add gameplay systems

    removeEntities(context.parent, entities) // remove title-screen entities

    fReadySystem.foreach(ref => {
      remSystems(context.parent, Set(self, ref)) // remove title-screen systems
    })

    // add player entities
    val fConnections: Set[(Entity, Future[ActorRef])] =
      for (e <- entities) yield
        (e, context.actorSelection(s"../connection-system/conn-${e.id.name}").resolveOne())

    for {
      (e, fConn) <- fConnections
      username = e.id.name
      config = gameplay.player(e.id)
      _ <- createEntities(context.parent, Set(config))
      conn <- fConn
    } {
      val inputSel = context.actorSelection(s"../input-$username")
      val observerSel = context.actorSelection(s"../observer-$username")

      for (ref <- inputSel.resolveOne) conn ! ref
      for (ref <- observerSel.resolveOne) ref ! conn
    }
  }

  override def updateEntities(ents: Set[Entity]): Unit = {
    entities = ents.filter(_.components.contains(Ready))
  }

  override def onTick(): Unit = if (!starting) {
    val setOfFutures: Set[Future[Snapshot]] =
      entities.map {
        e => (e(Ready) ? RequestSnapshot).mapTo[ReadyComponent.Snapshot]
      }

    val futureSet: Future[Set[Snapshot]] = Future.sequence(setOfFutures)

    // we should await the value so that we don't clash with future ticks
    val snaps: Set[Snapshot] = Await.result(futureSet, 1.second)

    if (snaps.forall(_.isReady) && snaps.size == MyGame.numPlayers) {
      starting = true
      startGame()
    }
  }
}
