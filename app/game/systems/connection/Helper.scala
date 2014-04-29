package game.systems.connection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{Props, PoisonPill, ActorRef, Actor}
import doppelengine.entity.EntityConfig
import doppelengine.core.Engine
import doppelengine.system.System.UpdateEntities
import akka.util.Timeout
import doppelengine.core.Engine.OpFailure

object Helper {
  def props(ngin: ActorRef, conn: ActorRef, numConns: Int, v: Long, config: EntityConfig) =
    Props(classOf[Helper], ngin, conn, numConns, v, config)
}

class Helper(ngin: ActorRef, conn: ActorRef, numConns: Int, var v: Long, config: EntityConfig) extends Actor {
  implicit val timeout: akka.util.Timeout = Timeout(1.second)

  def attempt() = {
    ngin ! Engine.Add(v, Set(config))
  }

  attempt()


  override def receive: Receive = {
    case correction: OpFailure =>
      v = correction.v
      attempt()

    case ack: Engine.OpSuccess =>
      self ! PoisonPill
      val inputSel = context.actorSelection(ngin.path / s"input_plr$numConns")
      val observerSel = context.actorSelection(ngin.path / s"observer_plr$numConns")

      for (ref <- inputSel.resolveOne) conn ! ref
      for (ref <- observerSel.resolveOne) ref ! conn
  }
}
