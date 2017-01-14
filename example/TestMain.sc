import $ivy.`net.bzzt::ectrace:0.1-SNAPSHOT`

import java.io.{BufferedWriter, File, FileWriter}

import scala.concurrent._
import scala.concurrent.duration._

import ectrace._

object TestMain extends App {
  implicit val ec: WrappedExecutionContext = WrappedExecutionContext(ExecutionContext.global)

  println("Start of test")
  val f = Range(0, 20).map(i ⇒ Future {
    println("Starting")
    Thread.sleep(4000)
    println("Done")
    s"Result $i"
  })
  f.map(_.map(println))
  Await.result(Future.sequence(f), 20.seconds)

  ec.dumpToFile("timeline.data")
}

TestMain.main(Array.empty)
