package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.ask
import game.systems.common.ConnectionSystem
import ConnectionSystem.AddPlayer
import play.api.mvc._
import akka.util.Timeout
import play.api.libs.iteratee.{Enumerator, Input, Done, Iteratee}
import game.MyGame
import play.api.libs.json.JsValue
import scala.concurrent.Future
import game.systems.common.ConnectionSystem

object PlayController extends Controller {

  import play.Logger

  implicit val timeout = Timeout(1.second)

  /** Serves the main page */
  def index = Action {
    Ok(views.html.index())
  }

  /**
   * WebSocket.async[JsValue] expects a function Request => (Iteratee[JsValue], Enumerator[JsValue]), where the
   * Iteratee[JsValue] handles incoming messages from the client, and the Enumerator[JsValue] pushes messages
   * to the client. Play will wire everything else together for us.
   *
   * In this case, we ask the engine to add a Player with username, and the engine sends back an Enumerator
   * and an ActorRef, which our Iteratee[JsValue] forwards all incoming data to.
   */
  def websocket(username: String) = WebSocket.async[JsValue] {
    implicit request =>
      Logger.info(s"$username requested WebSocket connection")
      (MyGame.connectionSystem ? AddPlayer(username)) map {

        case ConnectionSystem.Connected(connection, enumerator) => // Success
          val iter = Iteratee.foreach[JsValue] {
            connection ! _
          }
          (iter, enumerator)

        case ConnectionSystem.NotConnected(message) => // Connection error
          val iter = Done[JsValue, Unit]((), Input.EOF)
          val enum = Enumerator[JsValue](message).andThen(Enumerator.enumInput(Input.EOF))
          (iter, enum)
      }
  }
}