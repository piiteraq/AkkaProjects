package samples

import akka.actor.{ActorSystem, Kill, Props}

object KillActor extends App {
  val system = ActorSystem("KillTestSystem")
  val number5 = system.actorOf(Props[Number5], name="Number5")

  number5 ! "hello"
  // Send the kill message
  number5 ! Kill
  system.terminate
}
