package game.systems

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.pattern.ask
import akka.pattern.pipe
import doppelengine.component.Component
import game.components.io.ObserverComponent
import game.components.physics.DimensionComponent.Snapshot
import doppelengine.core.Engine.timeout
import doppelengine.entity.Entity
import doppelengine.entity.EntityId
import doppelengine.system.SystemBehavior
import game.components.types.{Observer, Dimension}

class VisualSystemBehavior extends SystemBehavior {

  var clients: Set[Entity] = Set()
  var visuals: Set[Entity] = Set()

  override def updateEntities(entities: Set[Entity]): Unit = {
    var newClients: Set[Entity] = Set()
    var newVisuals: Set[Entity] = Set()
    for (e <- entities) {
      if (e.components.contains(Dimension)) newVisuals += e
      if (e.components.contains(Observer)) newClients += e
    }
    clients = newClients
    visuals = newVisuals
  }

  override def onTick() = {
    val setOfFutures: Set[Future[(EntityId, Snapshot)]] =
      visuals.map(v => (v(Dimension) ? Component.RequestSnapshot).map {
        case snap: Snapshot => (v.id, snap)
      })

    val futureSet: Future[ObserverComponent.Update] =
      Future.sequence(setOfFutures).map {
        ObserverComponent.Update
      }

    for (c <- clients) futureSet.pipeTo(c(Observer))
  }
}