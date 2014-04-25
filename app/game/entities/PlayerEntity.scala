package game.entities

import doppelengine.component.ComponentType
import akka.actor.ActorRef
import doppelengine.entity.{EntityId, Entity}
import game.components.types.{Mobility, Dimension, Observer, Input}


class PlayerEntity( inputComponent: ActorRef,
                    observerComponent: ActorRef,
                    dimensionsComponent: ActorRef,
                    mobileComponent: ActorRef ) extends Entity {
  override val id: EntityId = EntityId( inputComponent.path.toString )

  override val components: Map[ ComponentType, ActorRef ] = Map(
    Input -> inputComponent,
    Observer -> observerComponent,
    Dimension -> dimensionsComponent,
    Mobility -> mobileComponent
  )
}