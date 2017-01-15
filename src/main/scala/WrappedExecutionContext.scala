package ectrace

import scala.concurrent._

case class WrappedExecutionContext(ec: ExecutionContext, context: List[String] = Nil) extends ExecutionContext {
  val created = System.currentTimeMillis()

  var scheduled = 0l
  var started = 0l
  var done = 0l

  var children: Seq[WrappedExecutionContext] = Nil

  def dumpToFile(filename: String): Unit = {
    import java.io.{BufferedWriter, File, FileWriter}

    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))

    children.foreach { ec ⇒
      val text: String = ec.created + " " + ec.scheduled + " " + ec.started + " " + ec.done + " " + ec.context.mkString("/")
      bw.write(text + "\n")
    }
    bw.close()
  }

  override def execute(runnable: Runnable): Unit = {
    scheduled = System.currentTimeMillis()
    ec.execute(new Runnable() {
      override def run() {
          started = System.currentTimeMillis()
          runnable.run()
          done = System.currentTimeMillis()
      }
    })
  }

  override def reportFailure(cause: Throwable): Unit = {
    ec.reportFailure(cause)
  }

  override def prepare(): ExecutionContext = {
    val newContext = describeCurrentStack() :: context
    val child = WrappedExecutionContext(ec.prepare, newContext)
    children = child +: children
    child
  }

  private def describeCurrentStack(): String = {
    describeStack(new Throwable().getStackTrace.drop(2))
  }

  private def describeStack(stack: Seq[StackTraceElement]): String = {
    if (stack.isEmpty)
      "Empty stack"
    else if (stack.head.getClassName.startsWith("scala."))
      describeStack(stack.tail)
    else {
      stack.head.toString
    }
  }
}
