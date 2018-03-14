package samples

import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object Common {
  val SleepTime = 5000
}

class BlockingActor extends Actor {
  def receive = {
    case i: Int ⇒
      Thread.sleep(Common.SleepTime) // Block - representing blocking I/O, etc
      println(s"Blocking operation finished: ${i}")
  }
}

class BlockingFutureActor extends Actor {
  implicit val executionContext: ExecutionContext = context.dispatcher // Won't work - needs separate dispatcher !
  def receive = {
    case i: Int ⇒
      println(s"Calling blocking Future: ${i}")
      Future {
        Thread.sleep(Common.SleepTime) // Block ..
        println(s"Blocking future finished ${i}")
      }
  }
}

class SeparateDispatcherFutureActor extends Actor {
  implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-blocking-dispatcher")
  def receive = {
    case i: Int ⇒
      println(s"Calling blocking Future: ${i}")
      Future {
        Thread.sleep(Common.SleepTime) // Block ..
        println(s"Blocking future finished ${i}")
      }
  }
}

class PrintActor extends Actor {
  def receive = {
    case i: Int ⇒
      println(s"PrintActor: ${i}")
  }
}

object DispatcherTest {

  def main(args: Array[String]) = {
    val system = ActorSystem("DispatcherTest")
    try {
      //val actor1 = system.actorOf(Props(new BlockingActor))
      //val actor1 = system.actorOf(Props(new BlockingFutureActor))
      val actor1 = system.actorOf(Props(new SeparateDispatcherFutureActor))

      val actor2 = system.actorOf(Props(new PrintActor))

      for (i ← 1 to 100) {
        actor1 ! i
        actor2 ! i
      }
    } finally {
      println(">>> Press ENTER to terminate ..")
      StdIn.readLine()
      system.terminate()
    }
  }

}