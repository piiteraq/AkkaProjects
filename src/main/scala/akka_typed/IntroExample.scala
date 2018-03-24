package akka_typed

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import scala.concurrent.ExecutionContext
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future


object HelloWorld {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String)

  val greeter = Behaviors.immutable[Greet] { (_, msg) ⇒
    println(s"Hello ${msg.whom}!")
    msg.replyTo ! Greeted(msg.whom)
    Behaviors.same
  }
}

object IntroExample extends App {

  import HelloWorld._

  // Using global pool since we want to run tasks after system.terminate
  import scala.concurrent.ExecutionContext.Implicits.global
  import akka.actor.Scheduler

  val system: ActorSystem[Greet] = ActorSystem(greeter, "hello")

  // '?' needs an implicit Timeout
  implicit val timeout: Timeout = Timeout(5 seconds)
  // '?' also needs an implicit Scheduler - it doesn't actually use it for anything,
  // so we can just create a bogus one.
  implicit val scheduler: Scheduler = new Scheduler {
    def maxFrequency: Double = 10

    def schedule( initialDelay: FiniteDuration,
                  interval:     FiniteDuration,
                  runnable:     Runnable)(implicit executor: ExecutionContext): Cancellable =
      new Cancellable {
        override def cancel(): Boolean = true
        override def isCancelled: Boolean = true
      }

    def scheduleOnce( delay:    FiniteDuration,
                      runnable: Runnable)(implicit executor: ExecutionContext): Cancellable =
      new Cancellable {
        override def cancel(): Boolean = true
        override def isCancelled: Boolean = true
      }
  }

  val future: Future[Greeted] = system ? (Greet("world", _))

  for {
    greeting ← future.recover { case ex ⇒ ex.getMessage }
    done ← {
      println(s"result: $greeting")
      system.terminate()
    }
  } println("system terminated")

}
