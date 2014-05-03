package game.systems

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{Props, actorRef2Scala}
import akka.pattern.ask
import doppelengine.component.Component
import game.components.io.InputComponent.Snapshot
import doppelengine.core.Engine
import doppelengine.entity.Entity
import doppelengine.system.System
import akka.util.Timeout
import game.components.types.{Observer, Input}

object QuitSystem {
  implicit val timeout = Timeout(1.second)

  def props = Props[QuitSystem]
}

class QuitSystem extends System(200.millis) {

  import QuitSystem.timeout

  val requiredComponents = List(Input, Observer)

  var entities: Set[Entity] = Set()

  override def updateEntities(ents: Set[Entity]): Unit =
    entities =
      ents.filter(_.hasComponents(requiredComponents))

  override def onTick(): Unit = {
    val setOfFutures: Set[Future[(Entity, Snapshot)]] =
      entities.map {
        e =>
          (e(Input) ? Component.RequestSnapshot)
            .mapTo[Snapshot]
            .map((e, _))
      }

    val futureSet: Future[Set[(Entity, Snapshot)]] = Future.sequence(setOfFutures)

    for (set <- futureSet) {
      val quitters = set.filter(_._2.quit).map(_._1)
      if (quitters.nonEmpty) context.parent ! Engine.Rem(version, quitters)
    }
  }
}