package samples

import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import concurrent.duration._
import language.postfixOps
import concurrent.{Await, ExecutionContext, Future}
import akka.pattern.ask
import akka.util.Timeout




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
    val fut: Future[Any] = myActor ? AskNameMessage

    // Get results asynchronously
    fut onComplete {
      case Success(x) => println(s"Success: $x")
      case Failure(ex) => println(s"Failed: $ex")
    }
    Thread.sleep(2000)

    // Get results synchronously
    //val result = Await.result(fut, timeout.duration).asInstanceOf[String]
    //println(result)
  } finally {
    system.terminate()
  }

}
