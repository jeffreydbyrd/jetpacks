package game.components.io

import play.api.libs.json.{Json, JsValue}

/**
 * A ServerCommand is a command that goes to the Server. Generally directed to a ClientProxy actor
 */
object ServerCommand {
  def getCommand(s: String): ServerCommand = getCommand(Json.parse(s))

  def getCommand(json: JsValue): ServerCommand = {
    val data = json \ "data"
    (json \ "type").as[String] match {
      case "LEFT" => GoLeft
      case "RIGHT" => GoRight
      case "UP" => Up
      case "DOWN" => Down
      case "STOP-LEFT" => StopLeft
      case "STOP-RIGHT" => StopRight
      case "STOP-UP" => StopUp
      case "STOP-DOWN" => StopDown
      case "QUIT" => ClientQuit
      case "click" =>
        val x = (data \ "x").as[Int]
        val y = (data \ "y").as[Int]
        Click(x, y)
      case s => Invalid(s)
    }
  }
}

trait ServerCommand

case object ClientQuit extends ServerCommand

case class Click(x: Int, y: Int) extends ServerCommand

case object Up extends ServerCommand

case object Down extends ServerCommand

case object GoLeft extends ServerCommand

case object GoRight extends ServerCommand

case object StopUp extends ServerCommand

case object StopLeft extends ServerCommand

case object StopDown extends ServerCommand

case object StopRight extends ServerCommand

case class Invalid(s: String) extends ServerCommand
