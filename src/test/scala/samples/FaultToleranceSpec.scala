package samples

import akka.actor.{ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.testkit.{EventFilter, ImplicitSender, TestActors, TestKit}


class FaultToleranceSpec(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem(
    "FaultHandlingDocSpec",
    ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
      }
      """)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A supervisor" must {
    "apply the chosen strategy for its child" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")

      // Have the supervisor actor create a new child and send back a reference to it
      supervisor ! Props[Child]
      val child = expectMsgType[ActorRef] // retrieve answer from TestKit’s testActor

      child ! 42 // set state to 42
      child ! "get"
      expectMsg(42)

      child ! new ArithmeticException // crash it
      child ! "get"
      expectMsg(42)

      child ! new NullPointerException // crash it harder
      child ! "get"
      expectMsg(0)

      watch(child) // Have testActor watch the “child”
      child ! new IllegalArgumentException // break it
      expectMsgPF() { case Terminated(`child`) ⇒ println("***** Child has terminated") }

      // Up to now the supervisor was completely unaffected by the child’s failure, because
      // the directives set did handle it. In case of an Exception, this is not true anymore
      // and the supervisor escalates the failure.
      supervisor ! Props[Child] // create new child
      val child2 = expectMsgType[ActorRef]
      child2 ! 9
      watch(child2)
      child2 ! "get" // verify it is alive
      expectMsg(9)

      // Escalate failure, which will restart the supervising actor and kill its children
      // due to default implementation of 'preRestart'
      child2 ! new Exception("CRASH")
      expectMsgPF() {
        case t @ Terminated(`child2`) if t.existenceConfirmed ⇒ println(s"***** Child crashed - failure escalated: $t")
      }

      // -----
      val supervisor2 = system.actorOf(Props[Supervisor2], "supervisor2")

      supervisor2 ! Props[Child]
      val child3 = expectMsgType[ActorRef]

      child3 ! 23
      child3 ! "get"
      expectMsg(23)

      // Due to overridden version of 'preRestart', children do not get killed when supervisor is restarted
      // but instead restarted. That will reset child3's state to '0'.
      child3 ! new Exception("CRASH")
      child3 ! "get"
      expectMsg(0)


    }
  }
}