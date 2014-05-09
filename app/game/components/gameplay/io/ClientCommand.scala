package game.components.gameplay.io

import game.components.gameplay.physics.Rect
import game.components.gameplay.physics.Position
import play.api.libs.json.{JsArray, JsNull, JsValue, Json}

/**
 * A Command that goes to the Client (ie. the browser)
 */
trait ClientCommand {
  val typ: String
  val doRetry: Boolean

  def toJson: JsValue
}

object ClientCommand {

  case object ServerQuit extends ClientCommand {
    override val typ = "quit"
    override val doRetry = false
    override val toJson = JsNull
  }

  case class CreateRect(id: String,
                        p: Position,
                        r: Rect) extends ClientCommand {
    override val doRetry: Boolean = true
    override val typ = "create"

    override val toJson = Json.obj(
      "id" -> id,
      "position" -> Json.arr(p.x, p.y),
      "dimensions" -> Json.arr(r.w, r.h)
    )
  }

  case class UpdatePositions(positions: Map[String, (Float, Float)],
                             override val doRetry: Boolean = false) extends ClientCommand {
    override val typ = "update-positions"

    override val toJson = {
      var json = Json.obj()

      for ((id, (x, y)) <- positions)
        json += (id -> Json.arr(x, y))

      json
    }
  }

  case class Move(id: String,
                  x: Float,
                  y: Float,
                  override val doRetry: Boolean = false) extends ClientCommand {
    override val typ = "move"

    override val toJson = Json.obj(
      "id" -> id,
      "position" -> Json.arr(x, y)
    )
  }

  case class UpdateIntro(statuses: Map[String, Boolean]) extends ClientCommand {
    override val typ: String = "update-intro"

    override val doRetry: Boolean = false

    override val toJson: JsValue = {
      var json = Json.arr()

      for ((name, isReady) <- statuses)
        json = json :+ Json.obj("name" -> name, "isReady" -> isReady)

      json
    }

  }

}