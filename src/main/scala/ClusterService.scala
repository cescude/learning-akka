import akka.actor._
import akka.routing._
import akka.cluster.routing._
import akka.cluster._
import akka.cluster.ClusterEvent._

object ClusterService {
  def apply(serviceName : String) : Props =
    Props(classOf[ClusterService], serviceName)
}

/** Identifies all nodes running the particularly named service on our cluster,
  * providing an interface to all through an AdaptiveLoadBalancing router.
  * 
  * This class allows a user to deploy more/less nodes into a cluster to meet
  * demand, while also maintaining multiple entry points that (each) load
  * balance across the cluster.
  */

class ClusterService(serviceName : String) extends Actor with Stash {
  val cluster = Cluster(context.system)

  val serviceNameElements = serviceName match {
    case RelativeActorPath(elements) => 
      elements
    case _ => 
      throw new IllegalArgumentException(
        s"[$serviceName] is not a valid relative actor path!")
  }

  def serviceAt(addr : Address) : ActorSelection =
    context.actorSelection(RootActorPath(addr) / serviceNameElements)

  override def preStart() = cluster.subscribe(self,
    classOf[MemberEvent],
    classOf[ReachabilityEvent])

  override def postStop() = cluster.unsubscribe(self)

  case class State(router : Router, is_stashing : Boolean = false) {
    def addAddress(address : Address) : State = {

      val new_worker = serviceAt(address)

      if (router.routees.contains(new_worker)) {
        this
      }
      else {
        State(router.addRoutee(new_worker), is_stashing)
      }
    }

    def dropAddress(address : Address) : State = 
      State(router.removeRoutee(serviceAt(address)), is_stashing)

    def stashing(yesno : Boolean) : State = State(router, yesno)

    def hasRoutes : Boolean = router.routees.nonEmpty
  }

  def next(state : State) : Receive = {

    // We only want identified actors in our router
    case ActorIdentity(address, Some(_)) =>

      if ( state.is_stashing ) {
        unstashAll()
      }

      context.become(next(state
        .addAddress(address.asInstanceOf[Address])
        .stashing(false)))

    case ActorIdentity(address, None) =>
      // The actor doesn't exist at this address.  So, do nothing with it.

    case MemberUp(m) =>
      serviceAt(m.address) ! Identify(m.address)

    case ReachableMember(m) =>
      serviceAt(m.address) ! Identify(m.address)

    case other : MemberEvent =>
      context.become(next(state.dropAddress(other.member.address)))

    case UnreachableMember(m) =>
      context.become(next(state.dropAddress(m.address)))

    case s : CurrentClusterState =>
      // Don't need this message

    case msg : Any if state.hasRoutes =>
      state.router.route(msg, sender())

    case msg  : Any =>
      stash()
      context.become(next(state.stashing(true)))
  }

  def receive = next(State(
    // Setup with an AdaptiveLoadBalancing router
    Router(AdaptiveLoadBalancingRoutingLogic(context.system))))
}
