package io.jobqueue.job

import java.time.Instant

trait JobContext {
  val jobId: JobId

  val enqueuedAt: Instant

  def configuration: JobConfiguration
}
