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
      case "QUIT" => ClientQuit
      case "LEFT" => GoLeft(true)
      case "RIGHT" => GoRight(true)
      case "UP" => Up(true)
      case "DOWN" => Down(true)
      case "ACTIVATE" => Activate(true)

      case "STOP-LEFT" => GoLeft(false)
      case "STOP-RIGHT" => GoRight(false)
      case "STOP-UP" => Up(false)
      case "STOP-DOWN" => Down(false)
      case "STOP-ACTIVATE" => Activate(false)
      case "click" =>
        val x = (data \ "x").as[Int]
        val y = (data \ "y").as[Int]
        Click(x, y)
      case s => Invalid(s)
    }
  }
}

trait ServerCommand

case class Activate(pressed:Boolean) extends ServerCommand

case object ClientQuit extends ServerCommand

case class Click(x: Int, y: Int) extends ServerCommand

case class Up(pressed: Boolean) extends ServerCommand

case class Down(pressed: Boolean) extends ServerCommand

case class GoLeft(pressed: Boolean) extends ServerCommand

case class GoRight(pressed: Boolean) extends ServerCommand

case class Invalid(s: String) extends ServerCommand
