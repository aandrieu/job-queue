package io.jobqueue.job

import scala.concurrent.duration._

final case class JobConfiguration(
  timeout:            Duration,
  maxNumberOfRetries: Int
)

object JobConfiguration {
  val default = JobConfiguration(
    timeout = 60 minutes,
    maxNumberOfRetries = 3
  )
}
