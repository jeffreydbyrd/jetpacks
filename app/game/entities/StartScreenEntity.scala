package game.entities

import doppelengine.entity.{EntityId, EntityConfig, Entity}
import doppelengine.component.ComponentConfig
import game.components.gameplay.io.InputComponent
import game.components.startscreen.{ReadyComponent, TitleObserverComponent}
import game.components.types.{Ready, TitleObserver, Input}

object StartScreenEntity {
  def config(id: String, name: String): EntityConfig = {
    val input =
      new ComponentConfig(InputComponent.props, s"input-$name")
    val observer =
      new ComponentConfig(TitleObserverComponent.props, s"observer-$name")
    val ready =
      new ComponentConfig(ReadyComponent.props, s"ready-$name")

    EntityConfig(
      EntityId(id, name),
      Map(
        Input -> input, TitleObserver -> observer, Ready -> ready
      )
    )

  }
}
