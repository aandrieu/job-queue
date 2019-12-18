package io.jobqueue.worker

sealed trait ExecutionResult

object ExecutionResult {
  final case object Success extends ExecutionResult
  final case object Failure extends ExecutionResult
}
