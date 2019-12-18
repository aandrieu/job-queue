package io.jobqueue.postgres.schema.migrations

import doobie.util.fragment.Fragment
import io.jobqueue.postgres.schema.Schema

private[postgres] object V0 extends Migration {
  val sql: Fragment =
    Fragment.const(
      s"""
         |CREATE TABLE IF NOT EXISTS "${Schema.jobTable}" (
         |  id BIGSERIAL,
         |  name TEXT,
         |  params JSON,
         |  enqueued_at TIMESTAMP WITH TIME ZONE
         |);
         |
         |CREATE INDEX IF NOT EXISTS "${Schema.jobTableIndex("next")}"
         |ON "${Schema.jobTable}" (enqueued_at);
         |""".stripMargin
    )
}
