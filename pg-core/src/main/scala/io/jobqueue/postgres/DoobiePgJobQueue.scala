package io.jobqueue.postgres

import cats.effect._
import io.jobqueue.NotUsed
import io.jobqueue.job._
import io.jobqueue.postgres.repository.JobQueueRepository
import io.jobqueue.postgres.schema.Schema
import io.jobqueue.postgres.util._
import io.jobqueue.queue.{ JobDecoder, JobEncoder }
import io.jobqueue.selection.Selection
import io.jobqueue.util.DateTimeService
import javax.sql.DataSource

private[postgres] class DoobiePgJobQueue[J <: Job](
  dataSource:         DataSource,
  transactorBuilder:  TransactorBuilder = TransactorBuilder,
  jobQueueRepository: JobQueueRepository = JobQueueRepository,
  dateTimeService:    DateTimeService = DateTimeService
)(
  implicit val encoder: JobEncoder[J],
  val decoder:          JobDecoder[J]
) extends PgJobQueue[J] {
  import doobie.implicits._

  private val transactor = transactorBuilder.fromDataSource(dataSource)
  private val jobMapper = new DatabaseJobMapper[J](dateTimeService)

  override def setupSchema(): IO[NotUsed] =
    transactor.use { xa =>
      Schema.migrations
        .map(_ => NotUsed)
        .transact(xa)
    }

  override def enqueue(
    job:           J,
    configuration: JobConfiguration = JobConfiguration.default
  ): IO[JobId] = {
    val databaseJob = jobMapper.toDatabaseJob(job, configuration)

    transactor.use { xa =>
      jobQueueRepository
        .enqueue(databaseJob)
        .transact(xa)
    }
  }

  override def next(selection: Selection = Selection.all): IO[Option[JobRef[J]]] =
    transactor.use { xa =>
      jobQueueRepository
        .next(selection)
        .map(jobMapper.toJobRef)
        .transact(xa)
    }

  override def release(jobRef: JobRef[J]): IO[NotUsed] =
    transactor.use { xa =>
      jobQueueRepository
        .release(jobRef.context.jobId)
        .map(_ => NotUsed)
        .transact(xa)
    }
}
