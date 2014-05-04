package game.components.io

import game.components.physics.Rect
import game.components.physics.Position
import play.api.libs.json.{JsNull, JsValue, Json}

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
    override val typ = "update_positions"

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

}