package io.jobqueue.selection

import scala.collection.immutable

final case class Selection(selectors: immutable.List[Selector[_]])

object Selection {
  val all = Selection()

  def apply(selector: Selector[_]*): Selection = new Selection(selector.toList)
}
