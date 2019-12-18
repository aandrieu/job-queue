package io.jobqueue.selection

import scala.collection.immutable

package object syntax {
  implicit final class SelectionOps(val selection: Selection) extends AnyVal {
    def and[U](selector: Selector[U]): Selection = Selection(
      selector :: selection.selectors
    )
  }

  implicit def toSelection[T](selector: Selector[T]): Selection = Selection(selector)

  implicit final class SelectorOps[T](val selector: Selector[T]) extends AnyVal {
    def and[U](other: Selector[U]): Selection = Selection(
      selector,
      other
    )
  }

  implicit final class StringCriteriaOps(val criteria: Criteria[String]) extends AnyVal {
    def in(values: immutable.List[String]): Selector[String] = StringSelector(
      criteria,
      Operator.Eq,
      OperatorMode.Any,
      values
    )

    def notIn(values: immutable.List[String]): Selector[String] = StringSelector(
      criteria,
      Operator.NotEq,
      OperatorMode.All,
      values
    )
  }
}
