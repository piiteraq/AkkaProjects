package samples

import akka.actor.Actor

class Kenny extends Actor {
  def receive = {
    case Explode => println("Now exploding .."); throw new Exception("Boom!")
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
