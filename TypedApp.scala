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

  case class HereWeAre(place : String, dist : Int)
  def whereAreWe(h : HereWeAre) : Unit = {
    println(s"Where are we?  $h")
  }
}

object TypedApp {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = new Timeout(60.seconds)

  def main(args : Array[String]) = {

    val system = ActorSystem("default")

    val doubler_ref = system.typedActorOf(MyFunctions.doubler)
    val printer_ref = system.typedActorOf(MyFunctions.printer)

    doubler_ref ! 3   // GOOD
    // doubler_ref ! "3" // Won't compile

    for (result <- doubler_ref ? 30) {

      // val compile_fail : String = result

      println(s"Doubled 30 and got $result, which is an Int, I hope (${result.isInstanceOf[Int]})")
    }

    printer_ref ! "yesterday!"   // GOOD
    // printer_ref ! 4.milliseconds // Won't compile

    val inline_ref = system.typedActorOf({ x : Int => x * 3 }) // This works too

    // inline_ref ! "BAH" // Won't compile

    // Use case classes, of course
    val where_ref = system.typedActorOf(MyFunctions.whereAreWe)

    import MyFunctions.HereWeAre
    where_ref ! HereWeAre("Miami", 10) 
  }
}
