package io.jobqueue.job.internal

import io.jobqueue.job.{ Job, JobContext, JobRef }

private[jobqueue] case class JobRefImpl[J <: Job](
  job:     J,
  context: JobContext
) extends JobRef[J]
