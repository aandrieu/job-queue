package io.jobqueue.worker.internal.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import io.jobqueue.job.{ Job, JobRef }
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.worker.internal.WorkerContext

private[actors] object JobQueueProxy {
  sealed trait Command
  case class FetchJob(sender: ActorRef[JobExecutor.Command]) extends Command
  case class ReleaseJob[J <: Job](
    jobRef: JobRef[J],
    sender: ActorRef[JobExecutor.Command]
  ) extends Command

  // TODO: integrate circuit breaker if no jobs
  // TODO: rewrite as class
  // TODO: rewrite with action / state diff method
  def apply[J <: Job](
    workerContext: WorkerContext,
    queue:         JobQueue[J],
    selection:     Selection
  ): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        import ctx.executionContext

        msg match {
          case FetchJob(sender) =>
            queue.next(selection).unsafeToFuture().map { jobRef =>
              sender ! JobExecutor.JobFetched(jobRef)
            }
            Behaviors.same

          case ReleaseJob(jobRef, sender) =>
            queue.release(jobRef.asInstanceOf[JobRef[J]]).unsafeToFuture().map { _ =>
              sender ! JobExecutor.JobReleased
            }
            Behaviors.same

          case _ =>
            Behaviors.unhandled
        }
    }
}
