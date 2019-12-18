package io.jobqueue.worker.internal.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import io.jobqueue.job.{ Job, JobRef }
import io.jobqueue.worker.internal.WorkerContext

import scala.concurrent.duration._

private[actors] object JobExecutor {
  sealed trait Command
  case object Start extends Command
  case object Stop extends Command
  case object FetchJob extends Command
  case class JobFetched[J <: Job](jobRef: Option[JobRef[J]]) extends Command
  case object JobReleased extends Command

  def apply(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command]
  ): Behavior[Command] = stopped(workerContext, queueSupervisor)

  private def stopped(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command]
  ): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case Start =>
            fetch(workerContext, queueSupervisor, ctx)

          case FetchJob =>
            Behaviors.ignore

          case _ =>
            Behaviors.unhandled
        }
    }

  private def started(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command]
  ): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case FetchJob =>
            fetch(workerContext, queueSupervisor, ctx)

          case Stop =>
            stopped(workerContext, queueSupervisor)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def fetch(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command],
    ctx:             ActorContext[Command]
  ): Behavior[Command] = {
    queueSupervisor ! JobQueueProxy.FetchJob(ctx.self)
    fetching(workerContext, queueSupervisor, toBeStopped = false)
  }

  private def fetching(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command],
    toBeStopped:     Boolean
  ): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case JobFetched(Some(jobRef)) =>
            run(workerContext, queueSupervisor, toBeStopped, ctx, jobRef)

          case JobFetched(None) if toBeStopped =>
            stopped(workerContext, queueSupervisor)

          case JobFetched(None) =>
            ctx.scheduleOnce(15 seconds, ctx.self, FetchJob)
            started(workerContext, queueSupervisor)

          case Stop =>
            fetching(workerContext, queueSupervisor, toBeStopped = true)

          case FetchJob =>
            Behaviors.ignore

          case _ =>
            Behaviors.unhandled
        }
    }

  private def run[J <: Job](
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command],
    toBeStopped:     Boolean,
    ctx:             ActorContext[Command],
    jobRef:          JobRef[J]
  ): Behavior[Command] = {
    import ctx.executionContext

    jobRef.run().unsafeToFuture().map { _ =>
      queueSupervisor ! JobQueueProxy.ReleaseJob(jobRef, ctx.self)
    }

    running(workerContext, queueSupervisor, toBeStopped)
  }

  private def running(
    workerContext:   WorkerContext,
    queueSupervisor: ActorRef[JobQueueProxy.Command],
    toBeStopped:     Boolean
  ): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case JobReleased if toBeStopped =>
            stopped(workerContext, queueSupervisor)

          case JobReleased =>
            fetch(workerContext, queueSupervisor, ctx)

          case Stop =>
            running(workerContext, queueSupervisor, toBeStopped = true)

          case FetchJob =>
            Behaviors.ignore

          case _ =>
            Behaviors.unhandled
        }
    }
}
