package io.jobqueue.job

import cats.effect.IO
import io.circe.Json
import io.jobqueue.worker.ExecutionResult

trait Job {
  def run(context: JobContext): IO[ExecutionResult]
}

final case class JsonJob(
  name:   String,
  params: Json
)
