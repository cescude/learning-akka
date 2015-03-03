import akka.actor._
import akka.pattern.ask
import akka.routing.BalancingPool
import akka.util.Timeout
import scala.concurrent.Future

object TypedActor {
  case class DataVal[A](msg : A)

  /** Wrapper that enforces type safety, as long as you don't get weird */
  class TypedActorRef[A,B](actorRef : ActorRef) {
    def tell(msg : A, sender : ActorRef) =
      actorRef.tell(DataVal(msg), sender)

    def !(msg : A)(implicit sender : ActorRef = Actor.noSender) = 
      tell(msg, sender)

    def ask(msg : A)(implicit timeout : Timeout) : Future[B] = 
      akka.pattern.ask(actorRef, DataVal(msg)).asInstanceOf[Future[B]]

    def ?(msg : A)(implicit timeout : Timeout) : Future[B] =
      ask(msg)
  }

  /** Convert a normal actor ref into a TypedActorRef.  Only use this if you KNOW
    * the actor ref points to a TypedActor, but for whatever reason you only
    * have access to the ref (e.g., when using remote actors).
    */
  def apply[A,B](actorRef : ActorRef) : TypedActorRef[A,B] = new TypedActorRef[A,B](actorRef)

  /** Props class used to create a particular TypedActor */
  private def props[A,B](fn : A => B) = Props(classOf[TypedActor[A,B]], fn)

  /** Extends an ActorSystem by allowing a user to directly create a TypedActorRef
    * from a function.
    *
    *  val system = ActorSystem("default")
    *  val my_actor = system.typedActorOf(2 * _)
    */
  implicit class TypedActorSystem(system : ActorSystem) {

    /** Returns a single TypedActorRef interfacing the given function */
    def typedActorOf[A,B](fn : A => B) : TypedActorRef[A,B] =
      new TypedActorRef(system.actorOf(props(fn)))

    def typedActorOf[A,B](fn : A => B, name : String) : TypedActorRef[A,B] = 
      new TypedActorRef(system.actorOf(props(fn), name))

    /** Returns a TypedActorRef interfacing a BalancingPool with the given number of
      * instances.  Note that because TypedActors don't rely on internal state,
      * a BalancingPool makes the most sense. */

    // TODO: Creating unnamed pools seems to crash at runtime, investigate to
    // figure out what I'm doing wrong here.

    // def typedActorPool[A,B](num_instances : Int, fn : A => B) : TypedActorRef[A,B] =
    //   new TypedActorRef(system.actorOf(
    //     BalancingPool(num_instances).props(props(fn))))

    def typedActorPool[A,B](num_instances : Int, fn : A => B, name : String) : TypedActorRef[A,B] =
      new TypedActorRef(system.actorOf(
        BalancingPool(num_instances).props(props(fn)), name))

  }
}

class TypedActor[A,B](operation : A => B) extends Actor {
  import TypedActor._

  def receive = {
    case DataVal(msg) =>
      val result : B = operation(msg.asInstanceOf[A])
      if ( !result.isInstanceOf[Unit] ) {
        sender() ! result
      }

    case anythingelse =>
      println(s"Whoa!, $anythingelse")
  }
}

