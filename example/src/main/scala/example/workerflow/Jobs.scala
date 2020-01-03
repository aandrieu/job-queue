package example.workerflow

import cats.effect.{ ContextShift, IO }
import io.circe.generic.JsonCodec
import io.jobqueue.job.{ ExecutionResult, Job, JobContext }
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object Jobs {
  sealed trait RootJob extends Job

  @JsonCodec
  final case class GreetJob(name: String) extends RootJob {
    override def run(context: JobContext): IO[ExecutionResult] = IO {
      println(s"Hello $name!!!")
      ExecutionResult.Success
    }
  }

  sealed trait NoParameterJob extends RootJob

  @JsonCodec
  final case class HelloWorldJob() extends NoParameterJob {
    override def run(context: JobContext): IO[ExecutionResult] = IO {
      println(s"Hello World!!!")
      ExecutionResult.Success
    }
  }

  sealed trait ApiJob extends RootJob

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  @JsonCodec
  final case class FetchTodoJob(id: Int) extends ApiJob {
    override def run(context: JobContext): IO[ExecutionResult] = {
      BlazeClientBuilder[IO](global).resource
        .use { httpClient =>
          httpClient.expect[String](s"https://jsonplaceholder.typicode.com/todos/$id")
        }.map { body =>
          println(body)
          ExecutionResult.Success
        }
    }
  }
}
