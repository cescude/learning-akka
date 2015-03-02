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

TODOC