import akka.actor._
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

import TypedActor._
import Repl.repl

//////////////////////////////////////////////////////////////////////////////

object ClusterServiceApp {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = new Timeout(60.seconds)

  def main(args : Array[String]) = {

    // Override on the commandline to start multiple entry points, e.g.:
    //
    // PORT=2552 sbt run ClusterServiceApp
    val port  = scala.sys.env.getOrElse("PORT", "2551")

    val port_cfg = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")

    val system = ActorSystem("default", port_cfg
      withFallback ConfigFactory.load())

    // Use ClusterService to find instances of the named actors in our cluster
    val doubler_ref = system.actorOf(ClusterService("/user/doubler"))
    val tripler_ref = system.actorOf(ClusterService("/user/tripler"))

    // Our workers were created as typed actors, so need to use a typed actor
    // ref to interact with them
    val doubler_svc = TypedActor[Int,Int](doubler_ref)
    val tripler_svc = TypedActor[Int,Int](tripler_ref)

    repl("Service >> ") {
      case ("double", vals) =>
        vals map (_.toInt) foreach { n =>
          for (result <- doubler_svc ? n) {
            println(s"2 * $n is $result!")
          }
        }

      case ("triple", vals) =>
        vals map (_.toInt) foreach { n =>
          for (result <- tripler_svc ? n) {
            println(s"3 * $n is $result!")
          }
        }
    }
  }
}
