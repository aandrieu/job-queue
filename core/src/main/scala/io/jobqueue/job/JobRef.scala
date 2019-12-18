package io.jobqueue.job

import cats.effect.IO
import io.jobqueue.worker.ExecutionResult

trait JobRef[J <: Job] {
  def context: JobContext

  def job: J

  def run(): IO[ExecutionResult] =
    job.run(context)
}
