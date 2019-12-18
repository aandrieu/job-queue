package io.jobqueue.worker.internal.actors

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import io.jobqueue.job.Job
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.worker.internal.actors
import io.jobqueue.worker.internal.WorkerContext

import scala.collection.immutable._

private[worker] object WorkerSupervisor {
  sealed trait Command
  case class Init[J <: Job](
    context:          WorkerContext,
    queue:            JobQueue[J],
    numberOfExecutor: Int,
    selection:        Selection
  ) extends Command
  case object Start extends Command
  case object Stop extends Command

  case class State(
    queueProxy: ActorRef[JobQueueProxy.Command],
    executors:  List[ActorRef[JobExecutor.Command]]
  )

  def apply(): Behavior[Command] =
    waitForInit()

  private def waitForInit(): Behavior[Command] =
    Behaviors.receive {
      case (ctx, msg) =>
        msg match {
          case Init(workerContext, queue, numberOfExecutor, selection) =>
            init(ctx, workerContext, queue, numberOfExecutor, selection)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def init[J <: Job](
    actorContext:     ActorContext[Command],
    workerContext:    WorkerContext,
    queue:            JobQueue[J],
    numberOfExecutor: Int,
    selection:        Selection
  ): Behavior[Command] = {
    val queueProxy = actorContext.spawn(
      actors.JobQueueProxy(workerContext, queue, selection),
      "job-queue-proxy"
    )

    val supervisedExecutor =
      Behaviors
        .supervise {
          JobExecutor(workerContext, queueProxy)
        }
        .onFailure(SupervisorStrategy.restart)

    val executors = (1 to numberOfExecutor).map { i =>
      actorContext.spawn(
        supervisedExecutor,
        s"job-executor-$i"
      )
    }

    val state = State(
      queueProxy,
      executors.toList
    )

    stopped(workerContext, state)
  }

  private def stopped(
    workerContext: WorkerContext,
    state:         State
  ): Behavior[Command] =
    Behaviors.receive {
      case (_, msg) =>
        msg match {
          case Start =>
            start(workerContext, state)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def start(
    workerContext: WorkerContext,
    state:         State
  ): Behavior[Command] = {
    state.executors.foreach { unit =>
      unit ! JobExecutor.Start
    }
    started(workerContext, state)
  }

  private def started(
    workerContext: WorkerContext,
    state:         State
  ): Behavior[Command] =
    Behaviors.receive {
      case (_, msg) =>
        msg match {
          case Stop =>
            stop(workerContext, state)

          case _ =>
            Behaviors.unhandled
        }
    }

  private def stop(
    workerContext: WorkerContext,
    state:         State
  ): Behavior[Command] = {
    state.executors.foreach { unit =>
      unit ! JobExecutor.Stop
    }
    stopped(workerContext, state)
  }
}
