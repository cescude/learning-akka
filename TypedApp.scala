import akka.actor._
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

import TypedActor._

//////////////////////////////////////////////////////////////////////////////

object MyFunctions {
  def doubler(n : Int) : Int = 2 * n
  def printer(s : String) : Unit = println(s"PRINTING $s")
}

object TypedApp {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = new Timeout(60.seconds)

  def main(args : Array[String]) : Unit = {

    val system = ActorSystem("default")

    val doubler_ref = system.typedActorOf(MyFunctions.doubler)
    val printer_ref = system.typedActorOf(MyFunctions.printer)

    doubler_ref ! 3
    doubler_ref ! "3" // ERROR #1

    for (result <- doubler_ref ? 30) {

      val compile_fail : String = result // ERROR #2

      println(s"Doubled 30 and got $result, which is an Int, I hope (${result.isInstanceOf[Int]})")
    }

    printer_ref ! "yesterday!"
    printer_ref ! 4.milliseconds // ERROR #3

    val inline_ref = system.typedActorOf({ x : Int => x * 3 }) // This works too

    inline_ref ! "BAH" // ERROR #4

    //////////////////////////////////////////////////////////////////////////

    // Additionally, we can create pools of typed actors via the
    // `typedActorPool' methods, and the interface via the TypedActorRef is
    // identical as above.

    // Make a pool with 5 members, all doubling in their free time
    val doubler_pool_ref = system.typedActorPool(5, MyFunctions.doubler, "doubler_pool")

    doubler_pool_ref ! 3
    doubler_pool_ref ! "Nope" // ERROR #5

    system.scheduler.schedule(0.milliseconds, 5.milliseconds) {
      doubler_pool_ref ! scala.util.Random.nextDouble() // ERROR #6
      doubler_pool_ref ! scala.util.Random.nextInt(200)
    }
  }
}
