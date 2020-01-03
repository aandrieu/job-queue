package io.jobqueue.worker.internal.actors

import java.util.UUID

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import io.jobqueue.job.{ Job, JobRef }
import io.jobqueue.util.Logging
import io.jobqueue.worker.{ ExecutorId, WorkerConfig }
import io.jobqueue.worker.internal.contexts.{ ExecutorLogContext, WorkerLogContext }

private[internal] object JobExecutor extends Logging {
  sealed trait Command
  case object Start extends Command
  case object FetchJob extends Command
  case class JobFetched[J <: Job](jobRef: Option[JobRef[J]]) extends Command
  case object JobReleased extends Command

  private case class State(
    workerContext:   WorkerLogContext,
    executorContext: ExecutorLogContext,
    queueProxy:      ActorRef[JobQueueProxy.Command],
    config:          WorkerConfig
  )

  def apply(
    workerContext: WorkerLogContext,
    queueProxy:    ActorRef[JobQueueProxy.Command],
    config:        WorkerConfig,
    uuid:          UUID = UUID.randomUUID(),
  ): Behavior[Command] =
    stopped(
      State(
        workerContext,
        ExecutorLogContext(
          ExecutorId(uuid),
          workerContext.workerId
        ),
        queueProxy,
        config
      )
    )

  private def stopped(state: State): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case Start =>
            fetch(ctx, state)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def fetch(
    actorContext: ActorContext[Command],
    state:        State
  ): Behavior[Command] = {
    logger.debug(
      "Fetch job",
      context = Some(state.executorContext)
    )

    state.queueProxy ! JobQueueProxy.FetchJob(state.executorContext, actorContext.self)

    fetching(state)
  }

  private def fetching(state: State): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case JobFetched(Some(jobRef)) =>
            run(ctx, state, jobRef)

          case JobFetched(None) =>
            ctx.scheduleOnce(state.config.fetchJobInterval, ctx.self, FetchJob)
            started(state)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def run[J <: Job](
    actorContext: ActorContext[Command],
    state:        State,
    jobRef:       JobRef[J]
  ): Behavior[Command] = {
    import actorContext.executionContext

    logger.debug(
      "Run job",
      info = jobRef.toLogMap,
      context = Some(state.executorContext)
    )

    jobRef.run().unsafeToFuture().map { _ =>
      logger.debug(
        "Release job",
        info = jobRef.toLogMap,
        context = Some(state.executorContext)
      )

      state.queueProxy ! JobQueueProxy.ReleaseJob(jobRef, state.executorContext, actorContext.self)
    }

    running(state)
  }

  private def running(state: State): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case JobReleased =>
            fetch(ctx, state)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def started(state: State): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case FetchJob =>
            fetch(ctx, state)

          case _ =>
            Behaviors.unhandled
        }
    }
}
