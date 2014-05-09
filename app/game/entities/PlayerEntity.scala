package game.entities

import doppelengine.component.ComponentType
import akka.actor.ActorRef
import doppelengine.entity.{EntityId, Entity}
import game.components.types.{Mobility, Dimension, GamePlayObserver, Input}


class PlayerEntity( inputComponent: ActorRef,
                    observerComponent: ActorRef,
                    dimensionsComponent: ActorRef,
                    mobileComponent: ActorRef ) extends Entity {
  override val id: EntityId = EntityId( inputComponent.path.toString )

  override val components: Map[ ComponentType, ActorRef ] = Map(
    Input -> inputComponent,
    GamePlayObserver -> observerComponent,
    Dimension -> dimensionsComponent,
    Mobility -> mobileComponent
  )
}