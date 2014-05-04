package game.components.io.connection

import game.components.io.ClientCommand
import play.api.libs.json.{Json, JsValue}

case object TestCommand extends ClientCommand {
  override val typ: String = "test"

  override def toJson: JsValue = Json.obj()

  override val doRetry: Boolean = true
}
