package game.components.io

import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import doppelengine.component.Component
import play.api.libs.json.JsValue

object InputComponent {
  val props = Props(classOf[InputComponent])

  // Sent
  case class Snapshot(activate: Boolean,
                      left: Boolean,
                      right: Boolean,
                      up: Boolean,
                      down: Boolean,
                      quit: Boolean)

}

class InputComponent extends Component {

  import Component._
  import InputComponent._

  var left = false
  var right = false
  var up = false
  var down = false
  var quit = false
  var activate = false

  def exec(cmd: ServerCommand) = cmd match {
    case Activate(pressed) => activate = pressed
    case Up(pressed) => up = pressed
    case Down(pressed) => down = pressed
    case GoLeft(pressed) => left = pressed
    case GoRight(pressed) => right = pressed
    case ClientQuit => quit = true
  }

  override def receive = LoggingReceive {
    case json: JsValue => exec(ServerCommand.getCommand(json))
    case RequestSnapshot =>
      sender ! Snapshot(activate, left, right, up, down, quit)
  }
}