package akka_streams

import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka_streams.AkkaStreamsQuickStart.{result, system}

import scala.util.Try

object AkkaStreamsQuickStart extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher // Execution context ~ thread pool
  implicit val materializer = ActorMaterializer() // We need an actor to run the stream ..

  val MaxFac = 100;

  // Ex1
  val source: Source[BigInt, NotUsed] = Source(BigInt(1) to MaxFac)
  //val done: Future[Done] = source.runForeach(i ⇒ println(i))(materializer)

  // Ex2: Factorials
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val factorials = source.scan(BigInt(1))((acc, next) ⇒ acc * next)
  val result: Future[Done] = {

    factorials
      .zipWith(Source(0 to MaxFac-1))((num, idx) ⇒ s"$idx! = $num")
      .throttle(5, 1.second, 1, ThrottleMode.shaping) // Throttle the flow. Throttle combinator adds back-pressure.
      .runForeach(println)

    //factorials.map(_.toString).runWith(lineSink("/Users/petec/factorials.txt"))
  }

  result.onComplete {
    res =>
      println(s"Result: $res")
      system.terminate()
  } // Remember to terminate the actor system ..
}

object TweetEx extends App {

  import akka.NotUsed
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._

  final case class Author(handle: String)
  final case class Hashtag(name: String)
  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil)

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher // Execution context ~ thread pool

  val result: Future[Done] = tweets
    .map(_.hashtags) // Get all sets of hashtags ...
    .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
    .mapConcat(identity) // Flatten the stream of tweets to a stream of hashtags
    .map(_.name.toUpperCase) // Convert all hashtags to upper case
    .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags

  // $FiddleDependency org.akka-js %%% akkajsactorstream % 1.2.5.1

  result.onComplete { _ => system.terminate() }
}

object StreamEx1 extends App {

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._

  implicit val system = ActorSystem("stream-ex1")
  implicit val materializer = ActorMaterializer()
  //implicit val ec = system.dispatcher // Execution context ~ thread pool

  val pickMaxOfThree = GraphDSL.create() { implicit b ⇒
    import GraphDSL.Implicits._

    val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
    val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }

  val resultSink = Sink.head[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b ⇒ sink ⇒
    import GraphDSL.Implicits._

    // importing the partial graph will return its shape (inlets & outlets)
    val pm3 = b.add(pickMaxOfThree)

    Source.single(1) ~> pm3.in(0)
    Source.single(2) ~> pm3.in(1)
    Source.single(3) ~> pm3.in(2)
    pm3.out ~> sink.in
    ClosedShape
  })

  val max: Future[Int] = g.run()
  println(Await.result(max, 300.millis)) //should equal(3)

  system.terminate()
}


object StreamEx2 extends App {

  import akka.NotUsed
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._

  implicit val system = ActorSystem("stream-ex1")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher // Execution context ~ thread pool

  val pairUpWithToString: Flow[Int, (Int, String), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      // prepare graph elements
      val broadcast = b.add(Broadcast[Int](2))
      val zip = b.add(Zip[Int, String]())

      // connect the graph
      broadcast.out(0).map(identity) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1

      // expose ports
      FlowShape(broadcast.in, zip.out)
    })

  val (_, result: Future[(Int, String)]) = pairUpWithToString.runWith(Source(List(3)), Sink.head)

  result.onComplete { res =>
    println(s"Res: $res")
    system.terminate()
  }

}


object StreamEx3 extends App {

  import akka.NotUsed
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._
  import scala.collection.immutable

  implicit val system = ActorSystem("stream-ex3")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher // Execution context ~ thread pool

  // A shape represents the input and output ports of a reusable
  // processing module
  case class PriorityWorkerPoolShape[In, Out](
                                               jobsIn:         Inlet[In],
                                               priorityJobsIn: Inlet[In],
                                               resultsOut:     Outlet[Out]) extends Shape {

    // It is important to provide the list of all input and output
    // ports with a stable order. Duplicates are not allowed.
    override val inlets: immutable.Seq[Inlet[_]] =
    jobsIn :: priorityJobsIn :: Nil
    override val outlets: immutable.Seq[Outlet[_]] =
      resultsOut :: Nil

    // A Shape must be able to create a copy of itself. Basically
    // it means a new instance with copies of the ports
    override def deepCopy() = PriorityWorkerPoolShape(
      jobsIn.carbonCopy(),
      priorityJobsIn.carbonCopy(),
      resultsOut.carbonCopy())

  }

  object PriorityWorkerPool {
    def apply[In, Out]( worker: Flow[In, Out, Any],
                        workerCount: Int): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {

      GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val priorityMerge = b.add(MergePreferred[In](1))
        val balance = b.add(Balance[In](workerCount))
        val resultsMerge = b.add(Merge[Out](workerCount))

        // After merging priority and ordinary jobs, we feed them to the balancer
        priorityMerge ~> balance

        // Wire up each of the outputs of the balancer to a worker flow
        // then merge them back
        for (i ← 0 until workerCount)
          balance.out(i) ~> worker ~> resultsMerge.in(i)

        // We now expose the input ports of the priorityMerge and the output
        // of the resultsMerge as our PriorityWorkerPool ports
        // -- all neatly wrapped in our domain specific Shape
        PriorityWorkerPoolShape(
          jobsIn = priorityMerge.in(0),
          priorityJobsIn = priorityMerge.preferred,
          resultsOut = resultsMerge.out)
      }

    }

    val worker1 = Flow[String].map("step 1 " + _)
    val worker2 = Flow[String].map("step 2 " + _)

    RunnableGraph.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val priorityPool1 = b.add(PriorityWorkerPool(worker1, 4))
      val priorityPool2 = b.add(PriorityWorkerPool(worker2, 2))

      Source(1 to 100).map("job: " + _) ~> priorityPool1.jobsIn
      Source(1 to 100).map("priority job: " + _) ~> priorityPool1.priorityJobsIn

      priorityPool1.resultsOut ~> priorityPool2.jobsIn
      Source(1 to 100).map("one-step, priority " + _) ~> priorityPool2.priorityJobsIn

      priorityPool2.resultsOut ~> Sink.foreach(println)
      ClosedShape
    }).run()

    //val result: NotUsed = g.run()

    //system.terminate()
  }

}


object StreamEx4 extends App {

  import akka.NotUsed
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl._
  import scala.collection.immutable

  implicit val system = ActorSystem("stream-ex4")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher // Execution context ~ thread pool

  import scala.concurrent.duration._
  case class Tick()

  RunnableGraph.fromGraph(GraphDSL.create() { implicit b ⇒
    import GraphDSL.Implicits._

    // this is the asynchronous stage in this graph
    val zipper = b.add(ZipWith[Tick, Int, Int]((tick, count) ⇒ count).addAttributes(Attributes.inputBuffer(initial = 1, max = 1)).async)

    Source.tick(initialDelay = 3.second, interval = 3.second, Tick()) ~> zipper.in0

    Source.tick(initialDelay = 1.second, interval = 1.second, "message!")
      .conflateWithSeed(seed = (_) ⇒ 1)((count, _) ⇒ count + 1) ~> zipper.in1

    zipper.out ~> Sink.foreach(println)
    ClosedShape
  }).run()

}