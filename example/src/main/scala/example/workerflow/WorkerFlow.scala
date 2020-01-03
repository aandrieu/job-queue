package example.workerflow

import akka.actor.ActorSystem
import example.Postgres
import example.workerflow.Jobs._
import io.jobqueue.derivation.{ DeriveJobDecoder, DeriveJobEncoder, JobsNameFor }
import io.jobqueue.postgres.PgJobQueue
import io.jobqueue.selection.Criteria.Job
import io.jobqueue.selection.syntax._
import io.jobqueue.worker.Worker

import scala.concurrent.Await
import scala.concurrent.duration._

object WorkerFlow extends App {
  val jobEncoder = DeriveJobEncoder[RootJob]
  val jobDecoder = DeriveJobDecoder[RootJob]

  val jobQueue = PgJobQueue[RootJob](Postgres.dataSource)(
    jobEncoder,
    jobDecoder
  )

  (for {
    _ <- jobQueue.setupSchema()

    _ <- jobQueue.enqueue(HelloWorldJob())

    _ <- jobQueue.enqueue(GreetJob("Alexis"))
    _ <- jobQueue.enqueue(GreetJob("Victor"))
    _ <- jobQueue.enqueue(GreetJob("Said"))
    _ <- jobQueue.enqueue(GreetJob("Matt"))
    _ <- jobQueue.enqueue(GreetJob("Manu"))

    _ <- jobQueue.enqueue(HelloWorldJob())

    _ <- jobQueue.enqueue(FetchTodoJob(1))
    _ <- jobQueue.enqueue(FetchTodoJob(2))
    _ <- jobQueue.enqueue(FetchTodoJob(3))
    _ <- jobQueue.enqueue(FetchTodoJob(4))
    _ <- jobQueue.enqueue(FetchTodoJob(5))

    _ <- jobQueue.enqueue(HelloWorldJob())
  } yield ()).unsafeRunSync()

  val selection = Job notIn JobsNameFor[NoParameterJob]

  implicit val actorSystem: ActorSystem = ActorSystem("worker-actor-system")

  val worker = Worker(jobQueue, selection)
  worker.start()

  Thread.sleep(10000)
  actorSystem.terminate()
  Await.ready(actorSystem.whenTerminated, 10 seconds)
}
