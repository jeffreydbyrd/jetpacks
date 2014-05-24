package game.systems.titlescreen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import doppelengine.system.System
import doppelengine.entity.{EntityId, Entity}
import doppelengine.component.ComponentType
import doppelengine.component.Component.RequestSnapshot
import game.components.types._
import akka.actor.{ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.Future
import game.components.common.io.InputComponent
import game.components.titlescreen.ReadyComponent
import game.components.titlescreen.ReadyComponent.Snapshot
import game.components.titlescreen.TitleObserverComponent.UpdateEntities

object ReadySystem {
  implicit val timeout: Timeout = 1.second

  def props = Props[ReadySystem]
}

class ReadySystem
  extends System(50.millis)
  with ActorLogging {

  import ReadySystem._

  val requirements: List[ComponentType] = List(Input, TitleObserver, Ready)
  var entities: Set[Entity] = Set()

  override def updateEntities(ents: Set[Entity]): Unit = {
    entities = ents.filter(_.hasComponents(requirements))
  }

  override def onTick(): Unit = {
    // Update the ReadyComponents
    for {
      e <- entities
      snapshot <- (e(Input) ? RequestSnapshot).mapTo[InputComponent.Snapshot]
    } if (snapshot.activate) {
      e(Ready) ! true
    }

    // Forward status of each entity to every other entity
    val setOfFutures: Set[Future[(EntityId, Snapshot)]] =
      for (e <- entities) yield
        (e(Ready) ? RequestSnapshot) map {
          case sn: ReadyComponent.Snapshot => (e.id, sn)
        }

    val futureUpdate: Future[UpdateEntities] =
      Future.sequence(setOfFutures).map(UpdateEntities)

    for (e <- entities)
      futureUpdate.pipeTo(e(TitleObserver))
  }

}
