package io.jobqueue.worker.internal.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import io.jobqueue.job.Job
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.util.Logging
import io.jobqueue.worker.WorkerConfig
import io.jobqueue.worker.internal.contexts.WorkerLogContext
import io.jobqueue.worker.internal.factories.{ BehaviorFactory, SupervisorStrategyFactory }

import scala.collection.immutable._

// TODO: Handle failures
private[worker] object WorkerSupervisor extends Logging {
  sealed trait Command
  case object Start extends Command

  def apply[J <: Job](
    workerContext:             WorkerLogContext,
    queue:                     JobQueue[J],
    selection:                 Selection,
    config:                    WorkerConfig,
    behaviorFactory:           BehaviorFactory,
    supervisorStrategyFactory: SupervisorStrategyFactory
  ): Behavior[Command] =
    Behaviors.setup { actorContext =>
      logger.debug(
        "Initializing worker supervisor",
        context = Some(workerContext)
      )

      val queueProxy = spawnJobQueueProxy(
        workerContext,
        actorContext,
        behaviorFactory,
        supervisorStrategyFactory,
        queue,
        selection,
        config
      )

      val jobExecutors = spawnJobExecutors(
        workerContext,
        actorContext,
        behaviorFactory,
        supervisorStrategyFactory,
        queueProxy,
        config
      )

      logger.debug(
        "Worker supervisor initialized",
        context = Some(workerContext)
      )

      stopped(workerContext, jobExecutors)
    }

  private def spawnJobQueueProxy[J <: Job](
    workerContext:             WorkerLogContext,
    actorContext:              ActorContext[WorkerSupervisor.Command],
    behaviorFactory:           BehaviorFactory,
    supervisorStrategyFactory: SupervisorStrategyFactory,
    queue:                     JobQueue[J],
    selection:                 Selection,
    config:                    WorkerConfig
  ): ActorRef[JobQueueProxy.Command] =
    actorContext.spawn(
      behaviorFactory.newSupervisedJobQueueProxy(
        workerContext,
        queue,
        selection,
        config,
        supervisorStrategyFactory.supervisorStrategyForJobQueueProxy
      ),
      "job-queue-proxy"
    )

  private def spawnJobExecutors(
    workerContext:             WorkerLogContext,
    actorContext:              ActorContext[WorkerSupervisor.Command],
    behaviorFactory:           BehaviorFactory,
    supervisorStrategyFactory: SupervisorStrategyFactory,
    queueProxy:                ActorRef[JobQueueProxy.Command],
    config:                    WorkerConfig
  ): List[ActorRef[JobExecutor.Command]] =
    (1 to config.numberOfExecutor).map { i =>
      actorContext.spawn(
        behaviorFactory.newSupervisedJobExecutor(
          workerContext,
          queueProxy,
          config,
          supervisorStrategyFactory.supervisorStrategyForJobExecutor
        ),
        s"job-executor-$i"
      )
    }.toList

  def stopped(
    workerContext: WorkerLogContext,
    executors:     List[ActorRef[JobExecutor.Command]]
  ): Behavior[Command] =
    Behaviors.receive {
      case (_, msg) =>
        msg match {
          case Start =>
            start(workerContext, executors)

          case _ =>
            Behaviors.unhandled
        }
    }

  def start(
    workerContext: WorkerLogContext,
    executors:     List[ActorRef[JobExecutor.Command]]
  ): Behavior[Command] = {
    logger.debug(
      "Starting worker supervisor"
    )

    executors.foreach { unit =>
      unit ! JobExecutor.Start
    }

    started(workerContext, executors)
  }

  def started(
    workerContext: WorkerLogContext,
    executors:     List[ActorRef[JobExecutor.Command]]
  ): Behavior[Command] =
    Behaviors.receive {
      case (_, msg) =>
        msg match {
          case _ =>
            Behaviors.unhandled
        }
    }
}
