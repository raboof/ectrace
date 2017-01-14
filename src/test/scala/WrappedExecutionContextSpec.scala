package ectrace

import scala.concurrent._
import scala.concurrent.duration._

import org.scalatest._

class WrappedExecutionContextSpec extends WordSpec with Matchers {
  "The wrapped executioncontext" should {
    "Correctly report on executing a single Future" in {
      implicit val ec = WrappedExecutionContext(ExecutionContext.global)

      Await.result(Future { 42 }, 1.second)

      ec.children.length should be(1)
      ec.children.head.context.length should be(1)
      ec.children.head.context.head should startWith("WrappedExecutionContextSpec.scala")
    }

    "Correctly report on mapping over a future" in {
      implicit val ec = WrappedExecutionContext(ExecutionContext.global)
      val future = Future { 42 }
      val mapped = future.map(_ * 2)
      Await.result(mapped, 1.second)

      ec.children.length should be(2)
      ec.children.head.context.length should be(1)
      val LineNo = ".*:(\\d+)".r
      val lineno = ec.children.head.context.head match {
        case LineNo(no) ⇒ no.toInt
      }

      ec.children.tail.head.context.length should be(1)
      ec.children.tail.head.context.head should be("WrappedExecutionContextSpec.scala:" + (lineno - 1))
    }
  }
}
