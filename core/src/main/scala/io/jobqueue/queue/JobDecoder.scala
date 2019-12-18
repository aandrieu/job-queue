package io.jobqueue.queue

import io.jobqueue.job.{ Job, JsonJob }

trait JobDecoder[J <: Job] {
  def decode(jsonJob: JsonJob): J
}
