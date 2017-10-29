/**
  * Created by petec on 1/8/17.
  */

import akka.actor._

case object Explode

class Kenny extends Actor {
  def receive = {
    case Explode => throw new Exception("Boom!")
    case msg => println(s"Kenny received a message: $msg")
  }
  override def preStart = { println("kenny::preStart") }
  override def postStop = { println("kenny::postStop") }
  override def preRestart(reason: Throwable, message: Option[Any]) = {
    println("kenny::preRestart")
    super.preRestart(reason, message)
  }
  override def postRestart(reason: Throwable) = {
    println("kenny::postRestart")
    super.postRestart(reason)
  }
}

class Parent extends Actor {
  val kenny = context.actorOf(Props[Kenny], name="Kenny")
  context.watch(kenny)

  def receive = {
    case Terminated(kenny) => println("OMG, they killed Kenny!")
    case _ => println("Parent received a message")
  }
}


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
