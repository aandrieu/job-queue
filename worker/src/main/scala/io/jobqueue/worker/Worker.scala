package io.jobqueue.worker

import java.util.UUID

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import io.jobqueue.job.Job
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.util.Logging
import io.jobqueue.worker.internal.actors.WorkerSupervisor
import io.jobqueue.worker.internal.contexts.WorkerLogContext
import io.jobqueue.worker.internal.factories.{ BehaviorFactory, SupervisorStrategyFactory }

final class Worker[J <: Job] private (
  queue:                     JobQueue[J],
  selection:                 Selection,
  config:                    WorkerConfig,
  uuid:                      UUID = UUID.randomUUID(),
  behaviorFactory:           BehaviorFactory = BehaviorFactory,
  supervisorStrategyFactory: SupervisorStrategyFactory = SupervisorStrategyFactory
)(
  implicit actorSystem: ActorSystem
) extends Logging {
  private val context = WorkerLogContext(
    WorkerId(uuid)
  )

  private val typedActorSystem = actorSystem.toTyped

  private val supervisor =
    typedActorSystem.systemActorOf(
      behaviorFactory.newWorkerSupervisor(
        context,
        queue,
        selection,
        config,
        behaviorFactory,
        supervisorStrategyFactory,
      ),
      s"job-queue-supervisor$$${uuid.toString}"
    )

  def start(): Unit =
    supervisor ! WorkerSupervisor.Start
}

object Worker {
  def apply[J <: Job](
    queue:     JobQueue[J],
    selection: Selection = Selection.all,
    config:    WorkerConfig = WorkerConfig
  )(
    implicit actorSystem: ActorSystem
  ): Worker[J] =
    new Worker[J](
      queue,
      selection,
      config
    )
}
