package io.jobqueue.worker.internal.contexts

import io.jobqueue.util.LogContext
import io.jobqueue.worker.{ ExecutorId, WorkerId }

private[worker] final case class ExecutorLogContext(
  executorId: ExecutorId,
  workerId:   WorkerId
) extends LogContext {
  override def toLogMap: Map[String, Any] = Map(
    "executor_id" -> executorId.value,
    "worker_id" -> workerId.value
  )
}
