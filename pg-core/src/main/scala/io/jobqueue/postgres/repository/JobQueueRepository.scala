package io.jobqueue.postgres.repository

import doobie._
import io.jobqueue.job.JobId
import io.jobqueue.postgres.model.{ DatabaseIdentifiedJob, DatabaseJob }
import io.jobqueue.postgres.schema.Schema
import io.jobqueue.postgres.util.{ DoobieCustomMappings, SqlSelectionCompiler }
import io.jobqueue.selection.Selection

private[postgres] class JobQueueRepository(
  sqlSelectionCompiler: SqlSelectionCompiler = SqlSelectionCompiler
) {
  import doobie.implicits._
  import DoobieCustomMappings._

  def enqueue(databaseJob: DatabaseJob): ConnectionIO[JobId] = {
    val sql =
      s"""
         |INSERT INTO "${Schema.jobTable}"
         |  (name, params, enqueued_at)
         |VALUES
         |  (?, ?, ?);
         |""".stripMargin

    Update[DatabaseJob](sql)
      .toUpdate0(databaseJob)
      .withGeneratedKeys[JobId]("id")
      .compile
      .lastOrError
  }

  def next(selection: Selection): ConnectionIO[Option[DatabaseIdentifiedJob]] = {
    val whereClause = if (selection.selectors.nonEmpty) {
      val sqlSelection = sqlSelectionCompiler.compile(selection)
      s"WHERE $sqlSelection"
    } else {
      ""
    }

    val sql =
      s"""
         |SELECT id, name, params, enqueued_at
         |FROM "${Schema.jobTable}"
         |$whereClause
         |ORDER BY enqueued_at
         |LIMIT 1
         |FOR UPDATE OF "${Schema.jobTable}" SKIP LOCKED;
         |""".stripMargin

    Query0[DatabaseIdentifiedJob](sql).stream.compile.last
  }

  def release(jobId: JobId): ConnectionIO[Int] = {
    val sql: String =
      s"""
         |DELETE FROM "${Schema.jobTable}"
         |WHERE id = ?;
         |""".stripMargin

    Update[Long](sql).run(jobId.value)
  }
}

private[postgres] object JobQueueRepository
    extends JobQueueRepository(
      SqlSelectionCompiler
    )
