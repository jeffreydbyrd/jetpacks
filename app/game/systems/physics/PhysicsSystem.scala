package game.systems.physics

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.actor.{Props, actorRef2Scala}
import akka.pattern.ask
import doppelengine.component.Component.RequestSnapshot
import game.components.physics.DimensionComponent
import game.components.physics.MobileComponent
import doppelengine.entity.Entity
import doppelengine.system.System
import game.components.io.InputComponent
import game.components.physics.Shape
import game.components.physics.Position
import game.components.types._
import akka.util.Timeout

object PhysicsSystem {
  implicit val timeout = Timeout(1.second)

  def props(gx: Int, gy: Int) = Props(classOf[PhysicsSystem], gx, gy)

  trait Data

  case class MobileData(e: Entity, p: Position, s: Shape, speed: Float, hops: Float) extends Data

  case class StructData(e: Entity, p: Position, s: Shape)

  def getStructData(structs: Set[Entity]): Set[Future[StructData]] =
    structs.map {
      e =>
        (e(Dimension) ? RequestSnapshot).map {
          case s: DimensionComponent.Snapshot => StructData(e, s.pos, s.shape)
        }
    }

  def getMobileData(mobs: Set[Entity]): Set[Future[MobileData]] =
    mobs.map(e => {
      val fDim = (e(Dimension) ? RequestSnapshot).mapTo[DimensionComponent.Snapshot]
      val fMob = (e(Mobility) ? RequestSnapshot).mapTo[MobileComponent.Snapshot]
      for (dim <- fDim; mob <- fMob)
      yield MobileData(e, dim.pos, dim.shape, mob.speed, mob.hops)
    })
}

class PhysicsSystem(gx: Int, gy: Int) extends System(20.millis) {

  import PhysicsSystem._

  val mobileComponents = List(Input, Dimension, Mobility)
  val simulation = new Box2dSimulation(gx, gy)

  var structures: Set[Entity] = Set()
  var mobiles: Set[Entity] = Set()

  override def updateEntities(ents: Set[Entity]) = {
    var newStructs: Set[Entity] = Set()
    var newMobiles: Set[Entity] = Set()

    for (e <- ents) {
      val comps = e.components
      if (e.hasComponents(mobileComponents)) newMobiles += e
      else if (comps.contains(Dimension)) newStructs += e
    }

    // delete mobiles
    for (e <- mobiles -- newMobiles) simulation.rem(e)

    // add mobiles
    for (futureMobile <- getMobileData(newMobiles -- mobiles)) {
      val data = Await.result(futureMobile, 1000 millis)
      simulation.createMobile(data)
    }

    // add structures
    for (futureStruct <- getStructData(newStructs -- structures))
      simulation.add(Await.result(futureStruct, 1000 millis))

    structures = newStructs
    mobiles = newMobiles
  }

  override def onTick() = {
    import InputComponent.Snapshot

    // Get inputs and apply them
    val futureSnaps: Set[(Entity, Future[Snapshot])] =
      for (e <- mobiles) yield
        (e, (e(Input) ? RequestSnapshot).mapTo[Snapshot])

    for ((e, fs) <- futureSnaps) {
      simulation.applyInputs(e, Await.result(fs, 1000 millis))
    }

    simulation.step()

    // Update the components with new positions
    for ((e, b2Mob) <- simulation.b2Mobiles) {
      val x = b2Mob.body.getPosition.x
      val y = b2Mob.body.getPosition.y
      e(Dimension) ! DimensionComponent.UpdatePosition(x, y)
    }
  }
}