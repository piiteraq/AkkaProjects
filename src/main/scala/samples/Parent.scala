package samples

import akka.actor.{Actor, Props, Terminated}

class Parent extends Actor {
  val kenny = context.actorOf(Props[Kenny], name="Kenny")
  context.watch(kenny)

  def receive = {
    case Terminated(kenny) => println("OMG, they killed Kenny!")
    case _ => println("Parent received a message")
  }
}
