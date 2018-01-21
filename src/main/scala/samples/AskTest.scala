package samples

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import concurrent.{Await, ExecutionContext, Future}
import concurrent.duration._
import language.postfixOps

case object AskNameMessage

class TestActor extends Actor {
  def receive = {
    case AskNameMessage => sender ! "Fred"
    case otherwise => println(s"Unknown message: $otherwise")
  }
}

object AskTest extends App {

  val system = ActorSystem("AskTestSystem")
  val myActor = system.actorOf(Props[TestActor], name="myActor")

  try {
    implicit val timeout = Timeout(5 seconds)
    val future = myActor ? AskNameMessage
    val result = Await.result(future, timeout.duration).asInstanceOf[String]
    println(result)
  } finally {
    system.terminate()
  }

}
