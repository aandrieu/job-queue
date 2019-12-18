package io.jobqueue.job.internal

import java.time.Instant

import io.jobqueue.job.{ JobConfiguration, JobContext, JobId }

private[jobqueue] case class JobContextImpl(
  jobId:         JobId,
  enqueuedAt:    Instant,
  configuration: JobConfiguration
) extends JobContext
