package game.entities

import akka.actor.ActorRef
import doppelengine.component.ComponentType
import doppelengine.entity.{EntityId, Entity}
import game.components.types.Dimension

class StructureEntity( physicalComponent: ActorRef ) extends Entity {
  override val id = EntityId( physicalComponent.path.toString )

  override val components: Map[ ComponentType, ActorRef ] = Map(
    Dimension -> physicalComponent
  )
}