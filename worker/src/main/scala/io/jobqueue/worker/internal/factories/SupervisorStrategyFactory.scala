package io.jobqueue.worker.internal.factories

import akka.actor.typed.SupervisorStrategy

import scala.concurrent.duration._

private[worker] trait SupervisorStrategyFactory {
  def supervisorStrategyForJobQueueProxy: SupervisorStrategy

  def supervisorStrategyForJobExecutor: SupervisorStrategy
}

private[worker] object SupervisorStrategyFactory extends SupervisorStrategyFactory {
  override val supervisorStrategyForJobQueueProxy: SupervisorStrategy =
    SupervisorStrategy
      .restartWithBackoff(
        minBackoff = 10 seconds,
        maxBackoff = 1 minute,
        randomFactor = 2
      )
      .withMaxRestarts(3)

  override val supervisorStrategyForJobExecutor: SupervisorStrategy =
    SupervisorStrategy.restart
}
