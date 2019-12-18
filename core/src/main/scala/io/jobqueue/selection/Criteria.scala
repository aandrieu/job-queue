package io.jobqueue.selection

sealed trait Criteria[T]

object Criteria {
  final case object Job extends Criteria[String]
}
