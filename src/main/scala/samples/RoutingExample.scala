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

// Example using routees created by the router itself
object RoutingExample {

  def main(args: Array[String]) = {

    val system = ActorSystem("RoutingExampleInternalRoutess")
    try {

      val master = system.actorOf(Props(new Master), "master")

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

// Example using externally created routees
class Workers extends Actor {
  context.actorOf(Props[Worker], name = "w1")
  context.actorOf(Props[Worker], name = "w2")
  context.actorOf(Props[Worker], name = "w3")

  override def receive = Map.empty // or '{ case _ => }'
}


object RoutingExampleExternalRoutees {

  def main(args: Array[String]) = {

    val system = ActorSystem("RoutingExampleExternalRoutees")

    try {

      // Method 1: create workers from config
      system.actorOf(Props[Workers], "workers") // Create workers before creating router
      val router3 = system.actorOf(FromConfig.props(), "router3") // Create the router

      // Method 2: create workers programmatically
      val paths = List("/user/workers/w1", "/user/workers/w2", "/user/workers/w3")
      val router4 = system.actorOf(RoundRobinGroup(paths).props(), "router4")

      println("***** Workers created by config")
      for (i ← 1 to 10) {
        router3 ! Work(s"By config: workload #$i")
      }

      println("***** Workers created programmatically")
      for (i ← 1 to 10) {
        router4 ! Work(s"Programmatically: workload #$i")
      }

    } finally {
      println(">>> Press ENTER to terminate ..")
      StdIn.readLine()
      system.terminate()
    }
  }

}

