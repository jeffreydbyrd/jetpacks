package game.components.io.connection

import game.components.io.ClientCommand

case object TestCommand extends ClientCommand {
  override val typ: String = "test"

  override def toJson: String = "{}"

  override val doRetry: Boolean = true
}
