import akka.actor._
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

import TypedActor._

//////////////////////////////////////////////////////////////////////////////

object ClusterServiceWorker {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = new Timeout(60.seconds)

  def doubler(n : Int) : Int = {
    println(s"...doubling $n")
    2 * n
  }
  def tripler(n : Int) : Int = {
    println(s"...tripling $n")
    3 * n
  }

  def main(args : Array[String]) = {

    val system = ActorSystem("default")

    val doubler_ref = system.typedActorOf(doubler, "doubler")
    val tripler_ref = system.typedActorOf(tripler, "tripler")
  }
}
