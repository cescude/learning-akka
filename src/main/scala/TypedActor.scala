import akka.actor._
import akka.pattern.ask

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
    * the actor ref points to a TypedActor, and even then, prefer using the
    * TypedActorSystem implicit class.
    */
  def apply[A,B](actorRef : ActorRef) : TypedActorRef[A,B] = new TypedActorRef[A,B](actorRef)

  /** Extends an ActorSystem by allowing a user to directly create a TypedActorRef
    * from a function.
    *
    *  val system = ActorSystem("default")
    *  val my_actor = system.typedActorOf(2 * _)
    */
  implicit class TypedActorSystem(system : ActorSystem) {
    def typedActorOf[A,B](fn : A => B, name : String) : TypedActorRef[A,B] = 
      new TypedActorRef[A,B](system.actorOf(Props(classOf[TypedActor[A,B]], fn), name))

    def typedActorOf[A,B](fn : A => B) : TypedActorRef[A,B] =
      new TypedActorRef[A,B](system.actorOf(Props(classOf[TypedActor[A,B]], fn)))
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

