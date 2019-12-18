package io.jobqueue.postgres

import cats.effect.IO
import io.jobqueue.NotUsed
import io.jobqueue.job.Job
import io.jobqueue.queue.{ JobDecoder, JobEncoder, JobQueue }
import javax.sql.DataSource

trait PgJobQueue[J <: Job] extends JobQueue[J] {
  def setupSchema(): IO[NotUsed]
}

object PgJobQueue {
  def apply[J <: Job](
    dataSource: DataSource
  )(
    implicit encoder: JobEncoder[J],
    decoder:          JobDecoder[J]
  ): PgJobQueue[J] = new DoobiePgJobQueue[J](dataSource)
}
