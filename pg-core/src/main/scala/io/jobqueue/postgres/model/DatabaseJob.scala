package io.jobqueue.postgres.model

import java.time.Instant

import io.circe.Json

private[postgres] case class DatabaseJob(
  name:       String,
  params:     Json,
  enqueuedAt: Instant
)
