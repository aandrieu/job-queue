package example.simpleflow

import cats.effect.IO
import example.Postgres
import io.circe.Json
import io.jobqueue.NotUsed
import io.jobqueue.job.{ ExecutionResult, Job, JobContext, JsonJob }
import io.jobqueue.postgres.PgJobQueue
import io.jobqueue.queue.{ JobDecoder, JobEncoder }

/**
  * The simple flow where everything is done by hand to
  * understand the core principles behind the lib.
  *
  * This code is here to show you the concepts behind the lib and
  * should not be used like that in production.
  *
  * Check the others examples to see a real production examples.
  */
object SimpleFlow extends App {
  // First we need to create a root for our hierarchy of jobs
  // Fow now, this only serve organizational purpose, but
  // you see on others examples that some macros rely on this
  // hierarchy to generate code
  sealed trait RootJob extends Job

  // This is our first Job. It must extends from our root and implements
  // a run method. This method is the behavior of our job and you can do
  // anything you want in it.
  final class GreetJob(val name: String) extends RootJob {
    override def run(context: JobContext): IO[ExecutionResult] = IO {
      println(s"Hello $name!!!")
      ExecutionResult.Success
    }
  }

  // Then, we must define a job encoder and a job decoder for our root.
  // These classes are used by the queue to serialize and to deserialize our jobs.
  // The queue representation of a job is called a JsonJob and is composed of two parts:
  //  - the name of the job ;
  //  - the parameters of the job as a json blob.

  // Encoder is responsible for converting a job of our hierarchy to a JsonJob.
  val jobEncoder = new JobEncoder[RootJob] {
    override def encode(job: RootJob): JsonJob = job match {
      case greetJob: GreetJob =>
        JsonJob(
          "MyGreetJob",
          Json.obj(
            "name" -> Json.fromString(greetJob.name)
          )
        )
      case _ => throw new RuntimeException(s"Unknown job: ${job}")
    }
  }

  // Decoder is responsible for converting a JsonJob to a job of our hierarchy.
  val jobDecoder = new JobDecoder[RootJob] {
    override def decode(jsonJob: JsonJob): RootJob = jsonJob.name match {
      case "MyGreetJob" =>
        new GreetJob(
          jsonJob.params.hcursor
            .get[String]("name")
            .getOrElse("")
        )
      case _ => throw new RuntimeException(s"Unknown job: ${jsonJob.name}")
    }
  }

  // Now, we have everything to create our queue
  // Our PG queue takes a DataSource. Like that, you can choose
  // to use connection pool our not.
  // Our queue takes also the encoder and the decoder that we have previously created.
  val jobQueue = PgJobQueue[RootJob](Postgres.dataSource)(
    jobEncoder,
    jobDecoder
  )

  // The queue system works with monad IO. Thus, we can
  // write simple program description and decide later how to execute it.
  // We will write a simple program that insert a job, execute it and release it.
  // This program will be run later synchronously.
  val example = for {
    // Since we work with a pg queue, the first step is to setup the schema.
    // This will apply all the migrations since the last setup
    // You should run this instruction before every other call of the queue
    // else it won't work as intended.
    // If all is already setup, this call do nothing.
    _ <- jobQueue.setupSchema()

    // Enqueue a GreetJob instance
    // This will not execute the job, only enqueue it
    jobId <- jobQueue.enqueue(new GreetJob("world"))
    _ = println(jobId)

    // Retrieve a ref to job. Since the queue was empty before we insert our job
    // this will retrieve the ref to our previously created job.
    // Passing by a ref is the only way to execute a job in the queue system.
    jobRefOpt <- jobQueue.next()
    _ = println(jobRefOpt)

    // Now, that we have a ref, let's run our job. To do thar only call the
    // run method of the ref. This will execute all the side effect of the job
    // and return an ExecutionResult. This result is not important for now,
    // but is important in a WorkerContext (see others examples).
    //
    // After the job is successfully executed, we need to release it from the queue
    // otherwise it will stay in the queue and may be retried later.
    jobResult <- jobRefOpt match {
      case Some(jobRef) =>
        for {
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

  // Last but not least, we run the program that we wrote.
  // You should see in the console a Greet call.
  example.unsafeRunSync()
}
