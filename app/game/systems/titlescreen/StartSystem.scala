package game.systems.titlescreen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import doppelengine.system.{SystemConfig, System}
import doppelengine.entity.{EntityConfig, EntityId, Entity}
import game.components.types.Ready
import akka.pattern.ask
import doppelengine.component.Component.RequestSnapshot
import akka.util.Timeout
import game.components.titlescreen.ReadyComponent
import scala.concurrent.{Await, Future}
import game.components.titlescreen.ReadyComponent.Snapshot
import game.MyGame
import game.systems.gameplay.VisualSystem
import akka.actor.{ActorLogging, PoisonPill, Props}
import game.entities.gameplay.GamePlay
import game.systems.gameplay.physics.PhysicsSystem

object StartSystem {
  val props = Props[StartSystem]
}

class StartSystem
  extends System(1.second)
  with ActorLogging {

  implicit val timeout: Timeout = 1.second

  var starting = false
  var entities: Set[Entity] = Set()

  val fReadySystem = context.actorSelection("../ready-system").resolveOne()

  def startGame(): Unit = {

    // Hold on to this snapshot of `entities`
    val titleEntities = entities

    val sysConfigs = Set(
      SystemConfig(VisualSystem.props, "visual-system"),
      SystemConfig(PhysicsSystem.props(0, -35), "physics-system")
    )

    addSystems(sysConfigs) // add gameplay systems

    // remove title-screen entities
    val fRemove: Future[Unit] = removeEntities(titleEntities)
    for {
      _ <- fRemove
      e <- titleEntities
      (_, comp) <- e.components
    } comp ! PoisonPill

    fReadySystem.foreach(ref => {
      remSystems(Set(self, ref)) // remove title-screen systems
      ref ! PoisonPill
    })

    // player configs
    val playerConfigs: Set[EntityConfig] =
      for {
        e <- titleEntities
        username = e.id.name
      } yield GamePlay.player(username)

    // wall configs
    val wallConfigs = Set(
      GamePlay.wall("floor", 25, 0, 50, 1),
      GamePlay.wall("ceiling", 25, 50, 50, 1),
      GamePlay.wall("left_wall", 0, 25, 1, 50),
      GamePlay.wall("right_wall", 50, 25, 1, 50)
    )

    // create gameplay entities
    val f: Future[Unit] = createEntities(playerConfigs ++ wallConfigs)
    for (_ <- f; e <- titleEntities) {
      val username = e.id.name
      val fConn = context.actorSelection(s"../connection-system/conn-$username").resolveOne()
      val fInput = context.actorSelection(s"../gameplay-input-$username").resolveOne()
      val fObserver = context.actorSelection(s"../gameplay-observer-$username").resolveOne()

      for (conn <- fConn) {
        for (input <- fInput) conn ! input
        for (observer <- fObserver) observer ! conn
      }
    }
  }

  override def updateEntities(ents: Set[Entity]): Unit = {
    entities = ents.filter(_.components.contains(Ready))
  }

  override def onTick(): Unit = if (!starting) {
    val setOfFutures: List[Future[Snapshot]] =
      entities.map {
        e => (e(Ready) ? RequestSnapshot).mapTo[ReadyComponent.Snapshot]
      }.toList

    val futureSet: Future[List[Snapshot]] = Future.sequence(setOfFutures)

    // we should await the value so that we don't clash with future ticks
    val snaps: List[Snapshot] = Await.result(futureSet, 1.second)

    if (snaps.forall(_.isReady) && snaps.size == MyGame.numPlayers) {
      starting = true
      startGame()
    }
  }

}
