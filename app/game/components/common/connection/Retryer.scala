package game.components.common.connection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.Props
import akka.event.LoggingReceive
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue

object Retryer {
  def props( msg: JsValue, channel: Channel[ JsValue ] ) = Props( classOf[ Retryer ], msg, channel )

  case object Retry
}

class Retryer( msg: JsValue, channel: Channel[ JsValue ] ) extends Actor {
  import Retryer._

  val retry = context.system.scheduler.schedule( 100 millis, 100 millis, self, Retry )

  override def receive = LoggingReceive {
    case Retry => channel push msg
  }

  override def postStop() = {
    retry.cancel()
  }
}