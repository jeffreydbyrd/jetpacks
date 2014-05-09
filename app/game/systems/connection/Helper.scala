package game.systems.connection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{Props, PoisonPill, ActorRef, Actor}
import doppelengine.entity.EntityConfig
import doppelengine.core.{EntityOpSuccess, EntityOpFailure, CreateEntities}
import akka.util.Timeout

object Helper {
  def props(engine: ActorRef, conn: ActorRef, numConns: Int, v: Long, config: EntityConfig) =
    Props(classOf[Helper], engine, conn, numConns, v, config)
}

class Helper(engine: ActorRef, conn: ActorRef, numConns: Int, var v: Long, config: EntityConfig) extends Actor {
  implicit val timeout: akka.util.Timeout = Timeout(1.second)

  def attempt() = {
    engine ! CreateEntities(v, Set(config))
  }

  attempt()

  override def receive: Receive = {
    case correction: EntityOpFailure =>
      v = correction.v
      attempt()

    case ack: EntityOpSuccess =>
      self ! PoisonPill
      val inputSel = context.actorSelection(engine.path / s"input-$numConns")
      val observerSel = context.actorSelection(engine.path / s"observer-$numConns")

      for (ref <- inputSel.resolveOne) conn ! ref
      for (ref <- observerSel.resolveOne) ref ! conn
  }
}
