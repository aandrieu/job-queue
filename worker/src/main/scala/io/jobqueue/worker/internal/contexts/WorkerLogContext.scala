package io.jobqueue.worker.internal.contexts

import io.jobqueue.util.LogContext
import io.jobqueue.worker.WorkerId

private[worker] final case class WorkerLogContext(
  workerId: WorkerId
) extends LogContext {
  override def toLogMap: Map[String, Any] = Map(
    "worker_id" -> workerId.value
  )
}
