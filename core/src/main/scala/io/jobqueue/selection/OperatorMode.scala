package io.jobqueue.selection

sealed trait OperatorMode {
  def rep: String
}

object OperatorMode {
  final case object Any extends OperatorMode {
    val rep = "ANY"
  }
  final case object All extends OperatorMode {
    val rep = "ALL"
  }
}
