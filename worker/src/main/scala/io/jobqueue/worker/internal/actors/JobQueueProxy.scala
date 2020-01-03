package io.jobqueue.worker.internal.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import io.jobqueue.job.{ Job, JobRef }
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.util.Logging
import io.jobqueue.worker.WorkerConfig
import io.jobqueue.worker.internal.contexts.{ ExecutorLogContext, WorkerLogContext }

private[internal] object JobQueueProxy extends Logging {
  sealed trait Command
  final case class FetchJob(
    context: ExecutorLogContext,
    sender:  ActorRef[JobExecutor.Command]
  ) extends Command
  final case object ExecuteFetchJobs extends Command
  final case class ReleaseJob[J <: Job](
    jobRef:  JobRef[J],
    context: ExecutorLogContext,
    sender:  ActorRef[JobExecutor.Command]
  ) extends Command

  private final case class State[J <: Job](
    waitingExecutors: List[ActorRef[JobExecutor.Command]],
    workerContext:    WorkerLogContext,
    queue:            JobQueue[J],
    selection:        Selection,
    config:           WorkerConfig
  )

  def apply[J <: Job](
    workerContext: WorkerLogContext,
    queue:         JobQueue[J],
    selection:     Selection,
    config:        WorkerConfig
  ): Behavior[Command] =
    Behaviors.setup { _ =>
      logger.debug(
        "Initializing job queue proxy",
        context = Some(workerContext)
      )

      waitForMessage(
        State(
          waitingExecutors = List.empty,
          workerContext,
          queue,
          selection,
          config
        )
      )
    }

  def waitForMessage[J <: Job](state: State[J]): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case FetchJob(executorContext, executorRef) =>
            debounceFetchJob(ctx, state, executorContext, executorRef)

          case ExecuteFetchJobs =>
            fetchJobs(ctx, state)

          case ReleaseJob(jobRef, executorContext, executorRef) =>
            releaseJob(
              ctx,
              state,
              jobRef.asInstanceOf[JobRef[J]],
              executorContext,
              executorRef
            )
        }
    }

  def debounceFetchJob[J <: Job](
    actorContext:    ActorContext[Command],
    state:           State[J],
    executorContext: ExecutorLogContext,
    executorRef:     ActorRef[JobExecutor.Command]
  ): Behavior[Command] = {
    logger.debug(
      s"Enqueuing job for future fetch",
      context = Some(executorContext)
    )

    if (state.waitingExecutors.isEmpty) {
      logger.debug(
        s"Triggering schedule for next batch fetch jobs",
        context = Some(state.workerContext)
      )

      actorContext.scheduleOnce(
        state.config.fetchJobDebounceDelay,
        actorContext.self,
        ExecuteFetchJobs
      )
    }

    waitForMessage(
      state.copy(
        waitingExecutors = executorRef :: state.waitingExecutors
      )
    )
  }

  def fetchJobs[J <: Job](
    actorContext: ActorContext[Command],
    state:        State[J]
  ): Behavior[Command] = {
    import actorContext.executionContext

    logger.debug(
      s"Fetching jobs",
      context = Some(state.workerContext)
    )

    state.queue
      .nextBatch(
        state.waitingExecutors.size,
        state.selection
      ).unsafeToFuture().foreach { jobRefs =>
        state.waitingExecutors.zipWithIndex.foreach {
          case (executorRef, index) =>
            val jobRef = jobRefs.lift(index)

            logger.debug(
              s"Job fetched",
              info = jobRef.map(_.toLogMap).getOrElse(Map.empty),
              context = Some(state.workerContext)
            )

            executorRef ! JobExecutor.JobFetched(jobRef)
        }
      }

    waitForMessage(
      state.copy(waitingExecutors = List.empty)
    )
  }

  def releaseJob[J <: Job](
    actorContext:    ActorContext[Command],
    state:           State[J],
    jobRef:          JobRef[J],
    executorContext: ExecutorLogContext,
    executorRef:     ActorRef[JobExecutor.Command]
  ): Behavior[Command] = {
    import actorContext.executionContext

    logger.debug(
      s"Releasing job",
      context = Some(executorContext)
    )

    state.queue.release(jobRef).unsafeToFuture().map { _ =>
      logger.debug(
        s"Job released",
        info = jobRef.toLogMap,
        context = Some(executorContext)
      )

      executorRef ! JobExecutor.JobReleased
    }

    Behaviors.same
  }
}
