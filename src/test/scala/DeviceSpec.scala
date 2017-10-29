package net.scidane.akka.sample

import akka.testkit.TestProbe
import org.scalatest.{FlatSpec, Matchers}
import akka.actor.ActorSystem


class DeviceSpec extends FlatSpec with Matchers {

  val system = ActorSystem("iot-system-test")

//  "A Device" should "reply with empty reading if no temperature is known" in {
//    val probe = TestProbe()
//    val deviceActor = system.actorOf(Device.props("group", "device"))
//
//    deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)
//    val response = probe.expectMsgType[Device.RespondTemperature]
//    response.requestId should ===(42)
//    response.value should ===(None)
//  }

}