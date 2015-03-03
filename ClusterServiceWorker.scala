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

  def slowdown[A,B](t : Duration, fn : A => B)(value : A) : B = {
    Thread.sleep(t.toMillis)
    fn(value)
  }

  def flaky[A,B](percent_flakiness : Double, e : Exception, fn : A => B)(value : A) : B = {
    if (scala.util.Random.nextFloat() < percent_flakiness) {
      throw e
    }
    fn(value)
  }

  def main(args : Array[String]) = {

    val system = ActorSystem("default")

    val doubler_ref = system.typedActorPool(5, doubler, "doubler")
    val tripler_ref = system.typedActorPool(5, tripler, "tripler")

    // val doubler_ref = system.typedActorPool(5, slowdown(2.seconds, doubler), "doubler")
    // val tripler_ref = system.typedActorPool(5, flaky(0.25, new ArithmeticException, tripler), "tripler")
  }
}
