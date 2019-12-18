package io.jobqueue.postgres.schema

import doobie._
import doobie.implicits._
import cats.implicits._
import doobie.free.connection
import io.jobqueue.postgres.schema.migrations._

private[postgres] object Schema {
  lazy val migrations: ConnectionIO[Int] = List(V0).foldLeft(
    connection.pure(0)
  ) {
    case (connection, migration) =>
      (connection, migration.sql.update.run).mapN(_ + _)
  }

  val jobTable = s"job_queue_jobs"

  val jobDeadLetter = s"job_queue_job_dead_letter"

  def jobTableIndex(indexName: String) =
    s"jobsqueue_jobs_${indexName}_idx"
}
