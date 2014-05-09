package game.components.startscreen

import akka.actor.{Props, Actor}
import doppelengine.component.Component.RequestSnapshot
import game.components.startscreen.ReadyComponent.Snapshot

object ReadyComponent {

  val props = Props[ReadyComponent]

  // Sent
  case class Snapshot(isRead: Boolean)

}

class ReadyComponent extends Actor {
  var isReady = false

  override def receive: Receive = {
    case r: Boolean => isReady = r
    case RequestSnapshot => sender ! Snapshot(isReady)
  }
}
