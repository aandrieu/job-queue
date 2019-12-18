package io.jobqueue.worker.internal

import io.jobqueue.worker.WorkerId

private[worker] final case class WorkerContext(
  id: WorkerId
)
