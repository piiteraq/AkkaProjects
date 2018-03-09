package samples

import akka.actor._

import scala.io.StdIn

object ActorHierarchyExperiments extends App {
  val system = ActorSystem("testSystem")

  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
  supervisingActor ! "failChild"

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally system.terminate()
}

