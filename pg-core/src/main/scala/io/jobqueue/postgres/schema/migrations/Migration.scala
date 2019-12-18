package io.jobqueue.postgres.schema.migrations

import doobie.util.fragment.Fragment

private[postgres] trait Migration {
  def sql: Fragment
}
