package example.advancedflow

import cats.effect.IO
import example.Postgres
import io.circe.generic.JsonCodec
import io.jobqueue.NotUsed
import io.jobqueue.derivation.{ DeriveJobDecoder, DeriveJobEncoder, JobsNameFor }
import io.jobqueue.job.{ ExecutionResult, Job, JobContext }
import io.jobqueue.postgres.PgJobQueue
import io.jobqueue.selection.Criteria._
import io.jobqueue.selection.syntax._

/**
  * The basic flow when using use the library with auto generation of code.
  */
object AdvancedFlow extends App {
  // In order to get access to the auto generation of encoder and decoder
  // your job hierarchy have to meet some criteria
  // 1. You have to define a root for your hierarchy
  sealed trait RootJob extends Job

  // 2. You can define any traits you want to categorize your jobs
  sealed trait NoParameterJob extends RootJob

  // 3. Your job have to be implemented as final case class and
  // must have circe encoder and decoder
  @JsonCodec
  final case class HelloWorldJob() extends NoParameterJob {
    override def run(context: JobContext): IO[ExecutionResult] = IO {
      println(s"Hello World!!!")
      ExecutionResult.Success
    }
  }

  @JsonCodec
  final case class GreetJob(name: String) extends RootJob {
    override def run(context: JobContext): IO[ExecutionResult] = IO {
      println(s"Hello $name!!!")
      ExecutionResult.Success
    }
  }

  // If you meet all the criteria above you can use these two macros
  // to automatically create the encoder and the decoder for you
  // based on your job root
  val jobEncoder = DeriveJobEncoder[RootJob]
  val jobDecoder = DeriveJobDecoder[RootJob]

  val jobQueue = PgJobQueue[RootJob](Postgres.dataSource)(
    jobEncoder,
    jobDecoder
  )

  // A selection is a list on constraints applied to job
  // Selections can be used un the next function of the queue
  // to only retrieve some kind of jobs
  // Here we create a selection that matches all jobs that extends NoParameterJob
  // The JobsNameFor macro returns the list of jobs that extends a certain trait
  val selection = Job in JobsNameFor[NoParameterJob]

  val example = for {
    _ <- jobQueue.setupSchema()

    // We enqueue two jobs to test the selection
    _ <- jobQueue.enqueue(GreetJob("JobQueue"))
    _ <- jobQueue.enqueue(HelloWorldJob())

    // Without selection the GreetJob should be picked
    // but with the selection we have restrained the search of next
    // to all the jobs that extend NoParameterJob
    jobRefOpt <- jobQueue.next(selection)
    _ = println(jobRefOpt)

    jobResult <- jobRefOpt match {
      case Some(jobRef) =>
        for {
          // Will print Hello world!!!
          result <- jobRef.run()
          _ <- jobQueue.release(jobRef)
        } yield {
          result
        }
      case None => IO.pure(ExecutionResult.Success)
    }
    _ = println(jobResult)
  } yield {
    NotUsed
  }

  example.unsafeRunSync()
}
