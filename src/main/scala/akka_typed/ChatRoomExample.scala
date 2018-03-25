package akka_typed

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import scala.concurrent.Await


sealed trait RoomCommand
final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand

sealed trait SessionEvent
final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
final case class SessionDenied(reason: String) extends SessionEvent
final case class MessagePosted(screenName: String, message: String) extends SessionEvent

trait SessionCommand
final case class PostMessage(message: String) extends SessionCommand
private final case class NotifyClient(message: MessagePosted) extends SessionCommand

object ChatRoom {

  private final case class PublishSessionMessage(screenName: String, message: String) extends RoomCommand

  val behavior: Behavior[RoomCommand] = chatRoom(List.empty)

  private def chatRoom(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] =
    Behaviors.immutable[RoomCommand] { (ctx, msg) ⇒
      msg match {
        case GetSession(screenName, client) ⇒
          // create a child actor for further interaction with the client
          val ses = ctx.spawn(
            session(ctx.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
          client ! SessionGranted(ses)
          chatRoom(ses :: sessions)
        case PublishSessionMessage(screenName, message) ⇒
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions foreach (_ ! notification)
          Behaviors.same
      }
    }

  private def session( room:       ActorRef[PublishSessionMessage],
                       screenName: String,
                       client:     ActorRef[SessionEvent] ): Behavior[SessionCommand] =
    Behaviors.immutable { (ctx, msg) ⇒
      msg match {
        case PostMessage(message) ⇒
          // from client, publish to others via the room
          room ! PublishSessionMessage(screenName, message)
          Behaviors.same
        case NotifyClient(message) ⇒
          // published from the room
          client ! message
          Behaviors.same
      }
    }
}

object ChatRoomExampleMain extends App {

  import scala.concurrent.duration._

  def messageGenerator(handle: ActorRef[PostMessage]) = {
    handle ! PostMessage("Hello World!")
    Thread.sleep(1000)
    handle ! PostMessage("I'm not the ol' gipper ..")
    Thread.sleep(1000)
    handle ! PostMessage(".. but rather the ol' gabbler")
    Thread.sleep(1000)
    handle ! PostMessage("Goodbye World!")
    Thread.sleep(1000)
  }

  val gabbler =
    Behaviors.immutable[SessionEvent] { (_, msg) ⇒
      msg match {
        case SessionGranted(handle: ActorRef[PostMessage]) ⇒
          messageGenerator(handle)
          Behaviors.same
        case MessagePosted(screenName, message) ⇒
          println(s"Message posted by '$screenName': $message")
          if (message.contains("Goodbye")) Behaviors.stopped else Behaviors.same
        case SessionDenied(reason) =>
          println(s"Session was denied, because: $reason")
          Behaviors.stopped
      }
    }

  val main: Behavior[NotUsed] =
    Behaviors.setup { ctx ⇒
      val chatRoom = ctx.spawn(ChatRoom.behavior, "chatroom")
      val gabblerRef = ctx.spawn(gabbler, "gabbler")
      ctx.watch(gabblerRef)
      chatRoom ! GetSession("ol’ Gabbler", gabblerRef)

      Behaviors.onSignal {
        case (_, Terminated(ref)) ⇒
          Behaviors.stopped
      }
    }

  val system = ActorSystem(main, "ChatRoomDemo")
  Await.result(system.whenTerminated, 10.seconds)

}
