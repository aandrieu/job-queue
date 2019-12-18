package io.jobqueue.selection

import scala.collection.immutable

sealed trait Selector[T] {
  val criteria: Criteria[T]
  val operator: Operator
  val operatorMode: OperatorMode
  val values: immutable.List[T]
}

final case class StringSelector(
  criteria:     Criteria[String],
  operator:     Operator,
  operatorMode: OperatorMode,
  values:       immutable.List[String]
) extends Selector[String]
