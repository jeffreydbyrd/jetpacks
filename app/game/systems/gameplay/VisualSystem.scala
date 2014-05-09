package game.systems.gameplay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.{ask,pipe}
import doppelengine.component.Component
import doppelengine.system.System
import game.components.gameplay.io.GamePlayObserverComponent
import game.components.gameplay.physics.DimensionComponent.Snapshot
import doppelengine.core.Engine.timeout
import doppelengine.entity.Entity
import doppelengine.entity.EntityId
import game.components.types.{GamePlayObserver, Dimension}
import akka.actor.Props

object VisualSystem {
  def props = Props[VisualSystem]
}

class VisualSystem extends System(16.millis) {

  var clients: Set[Entity] = Set()
  var visuals: Set[Entity] = Set()

  override def updateEntities(entities: Set[Entity]): Unit = {
    var newClients: Set[Entity] = Set()
    var newVisuals: Set[Entity] = Set()
    for (e <- entities) {
      if (e.components.contains(Dimension)) newVisuals += e
      if (e.components.contains(GamePlayObserver)) newClients += e
    }
    clients = newClients
    visuals = newVisuals
  }

  override def onTick() = {
    val setOfFutures: Set[Future[(EntityId, Snapshot)]] =
      visuals.map(v => (v(Dimension) ? Component.RequestSnapshot).map {
        case snap: Snapshot => (v.id, snap)
      })

    val futureSet: Future[GamePlayObserverComponent.UpdateEntities] =
      Future.sequence(setOfFutures).map {
        GamePlayObserverComponent.UpdateEntities
      }

    for (c <- clients) futureSet.pipeTo(c(GamePlayObserver))
  }
}