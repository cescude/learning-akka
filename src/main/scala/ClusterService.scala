import akka.actor._
import akka.routing._
import akka.cluster.routing._
import akka.cluster._
import akka.cluster.ClusterEvent._

object ClusterService {
  def apply(workerName : String) : Props =
    Props(classOf[ClusterService], workerName)

  // def apply(workerName : String)(implicit system : ActorSystem) : ActorRef =
  //   system.actorOf(Props(classOf[ClusterService], workerName), s"${workerName}_svc")
}

class ClusterService(workerName : String) extends Actor with Stash {
  val cluster = Cluster(context.system)

  val workerElements = workerName match {
    case RelativeActorPath(elements) => 
      elements
    case _ => 
      throw new IllegalArgumentException(
        s"[$workerName] is not a valid relative actor path!")
  }

  def worker(addr : Address) : ActorSelection =
    context.actorSelection(RootActorPath(addr) / workerElements)

  override def preStart() = cluster.subscribe(self,
    classOf[MemberEvent],
    classOf[ReachabilityEvent])

  override def postStop() = cluster.unsubscribe(self)

  def next(router : Router, been_stashing : Boolean) : Receive = {
    case ActorIdentity(address, Some(_)) =>
      val new_worker = worker(address.asInstanceOf[Address])

      if ( been_stashing ) {
        unstashAll()
      }

      if ( !router.routees.contains(new_worker) ) {
        context.become(next(router.addRoutee(new_worker), false))
      }
      else {
        context.become(next(router, false))
      }

    case ActorIdentity(address, None) =>
      // The actor doesn't exist at this address.  So, do nothing with it.

    case MemberUp(m) =>
      worker(m.address) ! Identify(m.address)

    case ReachableMember(m) =>
      worker(m.address) ! Identify(m.address)

    case other : MemberEvent =>
      context.become(next(router.removeRoutee(worker(other.member.address)), been_stashing))

    case UnreachableMember(m) =>
      context.become(next(router.removeRoutee(worker(m.address)), been_stashing))

    case s : CurrentClusterState =>
      // Don't need this

    case msg : Any if router.routees.size > 0 =>
      router.route(msg, sender())

    case msg  : Any =>
      stash()
      println(s"Stashed $msg")
      context.become(next(router, true))
  }

  def receive = next(Router(AdaptiveLoadBalancingRoutingLogic(context.system)), false)
}
