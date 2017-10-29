/**
  * Created by petec on 1/1/17.
  */

import akka.actor.{Actor, Props, ActorSystem}


class HelloActor(myName: String) extends Actor {

  def receive = {
    case "hello" => println(s"hello from $myName")
    case _ => println(s"'huh?', said $myName")
  }
}

object HelloActorMain {

  val system = ActorSystem("HelloSystem")
  val helloActor = system.actorOf(Props(new HelloActor("Fred")), name = "helloactor")

  def main(args: Array[String]): Unit = {

    helloActor ! "hello"
    helloActor ! "buenos dias"

    system.terminate()
  }

}