package io.jobqueue.selection

sealed trait Operator {
  def rep: String
}

object Operator {
  final case object Eq extends Operator {
    val rep = "="
  }
  final case object NotEq extends Operator {
    val rep = "!="
  }
}
