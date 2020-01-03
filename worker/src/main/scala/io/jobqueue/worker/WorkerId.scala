package io.jobqueue.worker

import java.util.UUID

final case class WorkerId(value: UUID) extends AnyVal
