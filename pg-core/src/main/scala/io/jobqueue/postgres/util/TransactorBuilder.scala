package io.jobqueue.postgres.util

import cats.effect.{ Blocker, ContextShift, IO, Resource }
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import javax.sql.DataSource

import scala.concurrent.ExecutionContext

private[postgres] class TransactorBuilder {
  type Transactor = Resource[IO, Aux[IO, DataSource]]

  def fromDataSource(
    dataSource:                    DataSource,
    nonBlockingOpExecutionContext: ExecutionContext = ExecutionContext.global,
    connectPoolThreadCount:        Int = 32
  ): Transactor = {
    implicit val nonBlockingOpContextShift: ContextShift[IO] =
      IO.contextShift(nonBlockingOpExecutionContext)

    val connectExecutionContext =
      ExecutionContexts.fixedThreadPool[IO](connectPoolThreadCount)
    val transactionBlocker = Blocker[IO]

    for {
      connectExecutionContext <- connectExecutionContext
      transactionBlocker <- transactionBlocker
    } yield {
      Transactor.fromDataSource[IO](
        dataSource,
        connectExecutionContext,
        transactionBlocker
      )
    }
  }
}

private[postgres] object TransactorBuilder extends TransactorBuilder
