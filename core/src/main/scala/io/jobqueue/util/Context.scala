package io.jobqueue.util

import io.jobqueue.worker.WorkerId

trait Context {
  def toLogMap: Map[String, Any]
}

private[jobqueue] final case class QueueContext(
  ) extends Context {
  override def toLogMap: Map[String, Any] = Map.empty
}

private[jobqueue] final case class WorkerContext(
  workerId: WorkerId
) extends Context {
  override def toLogMap: Map[String, Any] = Map(
    "worker_id" -> workerId.value
  )
}
