# learning-akka

Exploring different patterns with scala/akka.

## TypedActors

An interface to akka actors that still enforces some level of type safety.
Essentially, you pass a function (A => B) to an ActorSystem, and an actor is
created that enforces the function's declared type.  What follows is the gist of
how you use them, see the TypedApp.scala file for an actual example.

    object MyFunctions {
      def doubler(n : Int) : Int = 2 * n
      def printer(s : String) : Unit = println(s"PRINTING $s")
    }

    import TypedActor._

    val system = ActorSystem("default")

    val doubler_ref = system.typedActorOf(MyFunctions.doubler)
    val printer_ref = system.typedActorOf(MyFunctions.printer)

    doubler_ref ! 3   // GOOD
    doubler_ref ! "3" // Won't compile

    for (result <- doubler_ref ? 20) {
      // val nope : String = result // Won't compile
      println(s"Got $result back from the doubler actor thing")
    }   

    printer_ref ! "yesterday!"   // GOOD
    printer_ref ! 4.milliseconds // Won't compile

## ClusterService

Tracks nodes joining & leaving the cluster, Identifies the named actor and
provides a frontend to it.  This is an example of using an
AdaptiveLoadBalancingRoutingLogic router on a cluster (presumably correctly).

In code, the class is used as:

    val system = ActorSystem("default")

    // Returns an ActorRef that will route messages to & from any actors on the
    // cluster that are named "someactor"
    val task_ref = system.actorOf(ClusterService("/user/someactor"))
    
With the examples provided, start some workers in varying terminals:

    # term1
    $ sbt "runMain ClusterServiceWorker"

    # term2
    $ sbt "runMain ClusterServiceWorker"

Then start a copy of the ClusterServiceApp:

    # term3
    $ sbt "runMain ClusterServiceApp"

If you want another copy, do this:

    # term4
    $ PORT=2552 "runMain ClusterServiceApp"

From within the either of the ClusterServiceApp instances, you can type "triple
1 2 3 4", or "double 10 20 30" to see the workers run their thing.  You can also
add new workers at will, or Ctrl-C one of the existing workers to see that the
ClusterService is tracking things properly ("hopefully," at this stage of the
game).