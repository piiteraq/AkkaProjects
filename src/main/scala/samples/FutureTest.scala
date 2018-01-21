package samples

/**
  * Created by petec on 1/10/17.
  */

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

object FutureTest extends App {

  println("Starting calculation ..")
  val f = Future {
    Thread.sleep(Random.nextInt(500))
    42
  }

  println("Starting onComplete")
  f.onComplete {
    case Success(value) => println(s"Got the call-back, meaning = ${value}")
    case Failure(e) => e.printStackTrace
  }

  // Do the rest of your work
  println("A ..."); Thread.sleep(100)
  println("B ..."); Thread.sleep(100)
  println("C ..."); Thread.sleep(100)
  println("D ..."); Thread.sleep(100)
  println("E ..."); Thread.sleep(100)
  println("F ..."); Thread.sleep(100)
  println("G ..."); Thread.sleep(100)
  println("H ..."); Thread.sleep(100)
}


object FutureTest1 extends App {

  val f = Future {
   Thread.sleep(Random.nextInt(500))
    if (Random.nextInt(500) > 250) throw new Exception("Yikes!") else 42
  }

  f onSuccess {
    case result => println(s"Success: $result")
  }

  f onFailure {
    case t => println(s"Exception: ${t.getMessage}")
  }

  // Do the rest of your work
  println("A ..."); Thread.sleep(100)
  println("B ..."); Thread.sleep(100)
  println("C ..."); Thread.sleep(100)
  println("D ..."); Thread.sleep(100)
  println("E ..."); Thread.sleep(100)
  println("F ..."); Thread.sleep(100)
  println("G ..."); Thread.sleep(100)
  println("H ..."); Thread.sleep(100)
}


object Cloud {
  def runAlgorithm(i: Int): Future[Int] = Future {
    Thread.sleep(Random.nextInt(500))
    val result = i + 10
    println(s"Returning result from cloud: $result")
    result
  }
}

object RunningMultipleCalcs extends App {

  println("Starting futures")
  val result1 = Cloud.runAlgorithm(10)
  val result2 = Cloud.runAlgorithm(20)
  val result3 = Cloud.runAlgorithm(30)

  println("Before for-comprehension")
  val result: Future[Int] = for {
    r1 <- result1
    r2 <- result2
    r3 <- result3
  } yield (r1 + r2 + r3)

  println("Before onSuccess")
  result onSuccess {
    case result => println(s"Total = $result")
  }

  println("Before sleep at the end")
  Thread.sleep(2000)

}