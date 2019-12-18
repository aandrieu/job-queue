package io.jobqueue.util

import java.util.UUID

private[jobqueue] class UUIDService {
  def get(): UUID = UUID.randomUUID()
}

private[jobqueue] object UUIDService extends UUIDService
