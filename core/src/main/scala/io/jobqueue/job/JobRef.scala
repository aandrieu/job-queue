package io.jobqueue.job

import java.time.Instant

import cats.effect.IO
import io.jobqueue.util.LogContext

final case class JobRef[J <: Job](
  jobId:               JobId,
  enqueuedAt:          Instant,
  configuration:       JobConfiguration,
  private val job:     J,
  private val jsonJob: JsonJob
) extends JobContext
    with LogContext {
  def run(): IO[ExecutionResult] =
    job.run(this)

  def toLogMap: Map[String, Any] = Map(
    "job_id" -> jobId.value,
    "job_name" -> jsonJob.name,
    "job_params" -> jsonJob.params
  )
}
