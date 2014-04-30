package game.components.io.connection

import scala.concurrent.ExecutionContext.Implicits.global

import akka.testkit.{TestActorRef, TestProbe, TestKit}
import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import org.scalatest.{MustMatchers, FunSuiteLike, BeforeAndAfterAll}
import play.api.libs.json.JsValue
import play.api.libs.iteratee.Iteratee
import game.components.io.ClientCommand.ServerQuit

class PlayActorConnectionSpec
  extends TestKit(ActorSystem("EngineSpec"))
  with FunSuiteLike
  with MustMatchers
  with BeforeAndAfterAll {

  implicit val timeout: Timeout = 1.second

  val ackMessage = """{ "data" : 0, "type":"ack" }"""

  test("Retain any ActorRef I send it as a 'server actor'") {
    val (_, channel) = play.api.libs.iteratee.Concurrent.broadcast[String]
    val server = TestProbe()
    val connection = TestActorRef[PlayActorConnection](Props(classOf[PlayActorConnection], channel))
    connection ! server.ref

    connection.underlyingActor.toServer.get === server.ref
  }

  test("Parse and forward all JSON messages to the server actor as JsValues") {
    val (_, channel) = play.api.libs.iteratee.Concurrent.broadcast[String]
    val server = TestProbe()
    val connection = TestActorRef(Props(classOf[PlayActorConnection], channel))
    connection ! server.ref

    val message = """{ "type":"whatever", "data" : "whatever" }"""
    connection ! message
    server.expectMsgClass(classOf[JsValue])
  }

  test("Forward all ClientCommand messages to the channel") {
    val (enum, channel) = play.api.libs.iteratee.Concurrent.broadcast[String]

    val probe = TestProbe()
    enum(Iteratee.foreach[String] {
      probe.ref ! _
    })

    val server = TestProbe()
    val connection = TestActorRef(Props(classOf[PlayActorConnection], channel))
    connection ! server.ref

    connection ! ServerQuit
    probe.expectMsgPF() {
      case s: String if s.contains( """"type": "quit"""") => s
    }
  }

  test("Retry important messages until client responds with an ack message") {
    val (enum, channel) = play.api.libs.iteratee.Concurrent.broadcast[String]

    val probe = TestProbe()
    enum(Iteratee.foreach[String] {
      probe.ref ! _
    })

    val server = TestProbe()
    val connection = TestActorRef[PlayActorConnection](Props(classOf[PlayActorConnection], channel))
    connection ! server.ref

    connection ! TestCommand
    connection.underlyingActor.retryers.contains(0) mustBe true

    probe.expectMsgPF(500 millis) {
      case s: String if s.contains( """"type": "test""") => s
    }

    probe.expectMsgPF(500 millis) {
      case s: String if s.contains( """"type": "test""") => s
    }

    probe.expectMsgPF(500 millis) {
      case s: String if s.contains( """"type": "test""") => s
    }

    connection ! ackMessage
    connection.underlyingActor.retryers.contains(0) mustBe false
  }
}
