package io.jobqueue.worker.internal.factories

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import io.jobqueue.job.Job
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.worker.WorkerConfig
import io.jobqueue.worker.internal.actors.{ JobExecutor, JobQueueProxy, WorkerSupervisor }
import io.jobqueue.worker.internal.contexts.WorkerLogContext

trait BehaviorFactory {
  def newWorkerSupervisor[J <: Job](
    workerContext:             WorkerLogContext,
    queue:                     JobQueue[J],
    selection:                 Selection,
    config:                    WorkerConfig,
    behaviorFactory:           BehaviorFactory,
    supervisorStrategyFactory: SupervisorStrategyFactory
  ): Behavior[WorkerSupervisor.Command]

  def newJobQueueProxy[J <: Job](
    workerContext: WorkerLogContext,
    queue:         JobQueue[J],
    selection:     Selection,
    config:        WorkerConfig
  ): Behavior[JobQueueProxy.Command]

  def newSupervisedJobQueueProxy[J <: Job](
    workerContext:      WorkerLogContext,
    queue:              JobQueue[J],
    selection:          Selection,
    config:             WorkerConfig,
    supervisorStrategy: SupervisorStrategy
  ): Behavior[JobQueueProxy.Command] =
    Behaviors
      .supervise {
        this.newJobQueueProxy(
          workerContext,
          queue,
          selection,
          config
        )
      }
      .onFailure(supervisorStrategy)

  def newJobExecutor(
    workerContext: WorkerLogContext,
    queueProxy:    ActorRef[JobQueueProxy.Command],
    config:        WorkerConfig
  ): Behavior[JobExecutor.Command]

  def newSupervisedJobExecutor(
    workerContext:      WorkerLogContext,
    queueProxy:         ActorRef[JobQueueProxy.Command],
    config:             WorkerConfig,
    supervisorStrategy: SupervisorStrategy
  ): Behavior[JobExecutor.Command] =
    Behaviors
      .supervise {
        this.newJobExecutor(workerContext, queueProxy, config)
      }
      .onFailure(supervisorStrategy)
}

object BehaviorFactory extends BehaviorFactory {
  override def newWorkerSupervisor[J <: Job](
    workerContext:             WorkerLogContext,
    queue:                     JobQueue[J],
    selection:                 Selection,
    config:                    WorkerConfig,
    behaviorFactory:           BehaviorFactory,
    supervisorStrategyFactory: SupervisorStrategyFactory,
  ): Behavior[WorkerSupervisor.Command] =
    WorkerSupervisor(
      workerContext,
      queue,
      selection,
      config,
      behaviorFactory,
      supervisorStrategyFactory,
    )

  override def newJobQueueProxy[J <: Job](
    workerContext: WorkerLogContext,
    queue:         JobQueue[J],
    selection:     Selection,
    config:        WorkerConfig
  ): Behavior[JobQueueProxy.Command] =
    JobQueueProxy(
      workerContext,
      queue,
      selection,
      config
    )

  override def newJobExecutor(
    workerContext: WorkerLogContext,
    queueProxy:    ActorRef[JobQueueProxy.Command],
    config:        WorkerConfig
  ): Behavior[JobExecutor.Command] =
    JobExecutor(
      workerContext,
      queueProxy,
      config
    )
}
