package samples

import akka.actor.{ActorSystem, Props}

object DeathWatch extends App {
  val system = ActorSystem("DeathWatchTest")
  val parent = system.actorOf(Props[Parent], name="Parent")

  val kenny = system.actorSelection("/user/Parent/Kenny")

  kenny ! "Before explosion .."
  kenny ! Explode
  kenny ! "After explosion .."

  Thread.sleep(5000)
  println("Calling system.terminate")
  system.terminate
}
