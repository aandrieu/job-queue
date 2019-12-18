package io.jobqueue.postgres.model

import java.time.Instant

import io.circe.Json

private[postgres] case class DatabaseIdentifiedJob(
  id:         Long,
  name:       String,
  params:     Json,
  enqueuedAt: Instant
)
