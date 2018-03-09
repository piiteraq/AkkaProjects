import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActors, TestKit }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class TestSpec() extends TestKit(ActorSystem("TestSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {
    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }
  }
}
