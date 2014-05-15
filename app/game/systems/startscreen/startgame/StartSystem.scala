package game.systems.startscreen.startgame

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import doppelengine.system.System
import doppelengine.entity.Entity
import game.components.types.Ready
import akka.pattern.ask
import doppelengine.component.Component.RequestSnapshot
import akka.util.Timeout
import game.components.startscreen.ReadyComponent
import scala.concurrent.{Await, Future}
import game.components.startscreen.ReadyComponent.Snapshot
import game.MyGame

class StartSystem extends System(1.second) {
  implicit val timeout: Timeout = 1.second

  var starting = false
  var entities: Set[Entity] = Set()

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
    }
  }
}
