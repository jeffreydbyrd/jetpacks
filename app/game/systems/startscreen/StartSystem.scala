package game.systems.startscreen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import doppelengine.system.{SystemConfig, System}
import doppelengine.entity.{EntityConfig, EntityId, Entity}
import game.components.types.Ready
import akka.pattern.ask
import doppelengine.component.Component.RequestSnapshot
import akka.util.Timeout
import game.components.startscreen.ReadyComponent
import scala.concurrent.{Await, Future}
import game.components.startscreen.ReadyComponent.Snapshot
import game.MyGame
import game.systems.gameplay.VisualSystem
import akka.actor.{ActorLogging, PoisonPill, Props}
import game.entities.gameplay.GamePlay

object StartSystem {
  val props = Props[StartSystem]
}

class StartSystem
  extends System(500.millis)
  with ActorLogging {

  implicit val timeout: Timeout = 1.second

  var starting = false
  var entities: Set[Entity] = Set()

  val fReadySystem = context.actorSelection("../ready-system").resolveOne()

  def startGame(): Unit = {
    val sysConfigs = Set(
      SystemConfig(VisualSystem.props, "visual-system")
      //SystemConfig(PhysicsSystem.props(0, -35), "physics-system")
    )

    addSystems(sysConfigs) // add gameplay systems

    for {
      _ <- removeEntities(entities) // remove title-screen entities
      e <- entities
      (_, comp) <- e.components
    } comp ! PoisonPill

    fReadySystem.foreach(ref => {
      remSystems(Set(self, ref)) // remove title-screen systems
      ref ! PoisonPill
    })

    // add player entities
    val playerConfigs: Set[EntityConfig] =
      for {
        e <- entities
        username = e.id.name
      } yield GamePlay.player(username)

    val wallConfigs = Set(
      GamePlay.wall("floor", 25, 0, 50, 1),
      GamePlay.wall("ceiling", 25, 50, 50, 1),
      GamePlay.wall("left_wall", 0, 25, 1, 50),
      GamePlay.wall("right_wall", 50, 25, 1, 50)
    )

    val f: Future[Unit] = createEntities(playerConfigs ++ wallConfigs)
    Await.result(f, 1.second)

    for (e <- entities) {
      log.info("created entities")
      val username = e.id.name
      val fConn = context.actorSelection(s"../connection-system/conn-$username").resolveOne()
      for (conn <- fConn) {
        val fInput = context.actorSelection(s"../gameplay-input-$username").resolveOne()
        val fObserver = context.actorSelection(s"../gameplay-observer-$username").resolveOne()
        for (ref <- fInput) conn ! ref
        for (ref <- fObserver) ref ! conn
        log.info("sending connection...")
      }
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

  override def preStart() = {
    super.preStart()
    log.info("start-system started")
  }

  override def postStop() = {
    super.postStop()
    log.info("start-system stopped")
  }
}
