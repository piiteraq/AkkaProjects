package samples

import akka.actor.Actor

class Number5 extends Actor {
  def receive = {
    case _ => println("Number 5 got message")
  }

  override def preStart: Unit = { println("Number 5 is alive") }
  override def postStop: Unit = { println("Number5::postStop called") }
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("Number5::preRestart called")
  }
  override def postRestart(reason: Throwable) = { println("Number5::postRestart called") }
}
