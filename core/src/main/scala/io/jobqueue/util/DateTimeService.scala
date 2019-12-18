package io.jobqueue.util

import java.time.Instant

private[jobqueue] class DateTimeService {
  def now(): Instant = Instant.now()
}

private[jobqueue] object DateTimeService extends DateTimeService
