package io.jobqueue.queue

import cats.effect.IO
import io.jobqueue.NotUsed
import io.jobqueue.job._
import io.jobqueue.selection.Selection

trait JobQueue[J <: Job] {
  val encoder: JobEncoder[J]
  val decoder: JobDecoder[J]

  def enqueue(
    job:           J,
    configuration: JobConfiguration = JobConfiguration.default
  ): IO[JobId]

  def next(
    selection: Selection = Selection.all
  ): IO[Option[JobRef[J]]]

  def release(jobRef: JobRef[J]): IO[NotUsed]
}
