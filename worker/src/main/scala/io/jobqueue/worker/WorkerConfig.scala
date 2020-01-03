package io.jobqueue.worker

import com.github.andyglow.config._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

trait WorkerConfig {
  def fetchJobInterval: FiniteDuration
  def fetchJobDebounceDelay: FiniteDuration
  def numberOfExecutor: Int
}

object WorkerConfig extends WorkerConfig {
  private val config = ConfigFactory.load()

  val fetchJobInterval: FiniteDuration =
    config.getOrElse[FiniteDuration](
      "job-queue.worker.fetch-job-interval",
      5 seconds
    )

  val fetchJobDebounceDelay: FiniteDuration =
    config.getOrElse[FiniteDuration](
      "job-queue.worker.fetch-job-debounce-delay",
      100 milliseconds
    )

  val numberOfExecutor: Int =
    config.getOrElse[Int](
      "job-queue.worker.number-of-executor",
      1
    )
}
