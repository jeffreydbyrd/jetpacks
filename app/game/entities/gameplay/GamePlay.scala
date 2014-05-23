package game.entities.gameplay

import doppelengine.entity.{EntityId, EntityConfig}
import doppelengine.component.ComponentConfig
import game.components.common.io.InputComponent
import game.components.gameplay.GamePlayObserverComponent
import game.components.gameplay.physics.{MobileComponent, DimensionComponent}
import game.components.types.{Mobility, Dimension, GamePlayObserver, Input}

object GamePlay {
  def player(username: String): EntityConfig = {
    val input =
      new ComponentConfig(InputComponent.props, s"gameplay-input-$username")
    val observer =
      new ComponentConfig(GamePlayObserverComponent.props, s"gameplay-observer-$username")
    val dimensinos =
      new ComponentConfig(DimensionComponent.props(10, 10, 2, 2), s"gameplay-dimensions-$username")
    val mobility =
      new ComponentConfig(MobileComponent.props(20, 20), s"gameplay-mobile-$username")

    EntityConfig(
      EntityId(s"gameplay-$username", username),
      Map(
        Input -> input, GamePlayObserver -> observer,
        Dimension -> dimensinos, Mobility -> mobility
      )
    )
  }

  def wall(id: String, x: Float, y: Float, w: Float, h: Float) = {
    val dimensions: ComponentConfig = new ComponentConfig(DimensionComponent.props(x, y, w, h), id)
    EntityConfig(
      EntityId(id, id),
      Map(Dimension -> dimensions)
    )
  }
}
