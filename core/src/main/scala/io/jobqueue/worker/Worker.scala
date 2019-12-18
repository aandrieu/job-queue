package io.jobqueue.worker

import java.util.UUID

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import cats.effect.IO
import io.jobqueue.NotUsed
import io.jobqueue.job.Job
import io.jobqueue.queue.JobQueue
import io.jobqueue.selection.Selection
import io.jobqueue.worker.internal.WorkerContext
import io.jobqueue.worker.internal.actors.WorkerSupervisor

// TODO: Add logging
final class Worker[J <: Job] private (
  queue:            JobQueue[J],
  uuid:             UUID,
  numberOfExecutor: Int,
  selection:        Selection
)(
  implicit actorSystem: ActorSystem
) {
  private val context = WorkerContext(
    WorkerId(uuid)
  )

  private val typedActorSystem = actorSystem.toTyped

  private val supervisor =
    typedActorSystem.systemActorOf(
      WorkerSupervisor(),
      s"job-queue-supervisor$$${uuid.toString}"
    )
  supervisor ! WorkerSupervisor.Init(
    context,
    queue,
    numberOfExecutor,
    selection
  )

  def start(): IO[NotUsed] = IO {
    supervisor ! WorkerSupervisor.Start
    NotUsed
  }

  def stop(): IO[NotUsed] = IO {
    supervisor ! WorkerSupervisor.Stop
    NotUsed
  }
}

object Worker {
  def apply[J <: Job](
    queue:            JobQueue[J],
    uuid:             UUID = UUID.randomUUID(),
    numberOfExecutor: Int = 1,
    selection:        Selection = Selection.all
  )(
    implicit actorSystem: ActorSystem
  ): Worker[J] =
    new Worker[J](
      queue,
      uuid,
      numberOfExecutor,
      selection
    )
}
