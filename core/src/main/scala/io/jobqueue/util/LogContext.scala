package io.jobqueue.util

private[jobqueue] trait LogContext {
  def toLogMap: Map[String, Any]
}
