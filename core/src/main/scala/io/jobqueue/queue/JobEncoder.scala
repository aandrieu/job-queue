package io.jobqueue.queue

import io.jobqueue.job.{ Job, JsonJob }

trait JobEncoder[J <: Job] {
  def encode(job: J): JsonJob
}
