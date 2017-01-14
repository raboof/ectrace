
package ectrace

import java.io.{BufferedWriter, File, FileWriter}

import scala.concurrent._
import scala.concurrent.duration._

import org.scalatest._

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

  val file = new File("timeline.data")
  val bw = new BufferedWriter(new FileWriter(file))

  ec.children.foreach { ec ⇒
    val text: String = ec.created + " " + ec.scheduled + " " + ec.started + " " + ec.done + " " + ec.context.mkString("/")
    println(text)
    bw.write(text + "\n")
  }
  bw.close()
}
