package game.systems

import scala.concurrent.duration._
import doppelengine.system.System
import doppelengine.entity.Entity
import game.components.types._
import doppelengine.component.ComponentType

object IntroSystem {

}

class IntroSystem extends System(200.millis) {
  val requirements: List[ComponentType] = List(Input, Observer, Ready)
  val entities: Set[Entity] = Set()

  override def updateEntities(entities: Set[Entity]): Unit = {
    
  }

  override def onTick(): Unit = {}
}
