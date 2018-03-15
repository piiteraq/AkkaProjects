package samples

/**
  * Routing Example from Akka User Guide
  */

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.routing._

import scala.io.StdIn

case class Work(content: String)

class Worker extends Actor {
  def receive = {
    case w: Work => println(s"Worker ${self.path} received ${w.content} from ${sender().path}")
    case otherwise => println("Error: Unknown message ..")
  }
}

class Master extends Actor {

  var router = {

    val rout = (1 to 5).map { i =>
      val r = context.actorOf(Props[Worker], s"Worker_$i")
      context watch r
      ActorRefRoutee(r)
    }

    Router(RoundRobinRoutingLogic(), rout)
  }

  def receive = {
    case w: Work ⇒
      router.route(w, sender())
    case Terminated(a) ⇒
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[Worker])
      context watch r
      router = router.addRoutee(r)
  }
}

object RoutingExample {

  def main(args: Array[String]) = {

    val system = ActorSystem("RoutingExample")
    try {

      //val master = system.actorOf(Props(new Master), "master") // Method1: From Master class ..
      //val master = system.actorOf(FromConfig.props(Props[Worker]), "master") // Method2: From config file ..
      val master: ActorRef = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "master") // Method3: Router configuration provided programmatically ..

      for (i ← 1 to 100) {
        master ! Work(s"workload #$i")
      }

    } finally {
      println(">>> Press ENTER to terminate ..")
      StdIn.readLine()
      system.terminate()
    }
  }

}