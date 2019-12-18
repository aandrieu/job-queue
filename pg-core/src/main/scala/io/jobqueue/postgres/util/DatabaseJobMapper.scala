package io.jobqueue.postgres.util

import io.jobqueue.job.internal.{ JobContextImpl, JobRefImpl }
import io.jobqueue.job.{ Job, JobConfiguration, JobId, JobRef, JsonJob }
import io.jobqueue.postgres.model.{ DatabaseIdentifiedJob, DatabaseJob }
import io.jobqueue.queue.{ JobDecoder, JobEncoder }
import io.jobqueue.util.DateTimeService

private[postgres] class DatabaseJobMapper[J <: Job](
  dateTimeService: DateTimeService = DateTimeService
)(
  implicit encoder: JobEncoder[J],
  decoder:          JobDecoder[J]
) {
  def toDatabaseJob(job: J, configuration: JobConfiguration): DatabaseJob = {
    val jsonJob = encoder.encode(job)
    DatabaseJob(
      jsonJob.name,
      jsonJob.params,
      dateTimeService.now()
    )
  }

  def toJobRef(databaseIdentifiedJob: Option[DatabaseIdentifiedJob]): Option[JobRef[J]] =
    databaseIdentifiedJob.map(toJobRef)

  private def toJobRef(databaseIdentifiedJob: DatabaseIdentifiedJob): JobRef[J] = {
    val jsonJob = JsonJob(
      databaseIdentifiedJob.name,
      databaseIdentifiedJob.params
    )
    val job = decoder.decode(jsonJob)
    JobRefImpl(
      job,
      JobContextImpl(
        JobId(databaseIdentifiedJob.id),
        databaseIdentifiedJob.enqueuedAt,
        JobConfiguration.default
      )
    )
  }
}
