/**
  * Created by petec on 1/8/17.
  */

import akka.actor._

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

object KillActor extends App {
  val system = ActorSystem("KillTestSystem")
  val number5 = system.actorOf(Props[Number5], name="Number5")

  number5 ! "hello"
  // Send the kill message
  number5 ! Kill
  system.terminate
}
