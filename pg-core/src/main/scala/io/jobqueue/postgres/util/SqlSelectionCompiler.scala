package io.jobqueue.postgres.util

import java.util.concurrent.ConcurrentHashMap

import io.jobqueue.selection.{ Criteria, Selection, StringSelector }

private[postgres] class SqlSelectionCompiler {
  private val cache = new ConcurrentHashMap[Selection, String]()

  def compile(selection: Selection): String =
    cache.computeIfAbsent(selection, selectionToSql)

  private def selectionToSql(selection: Selection): String =
    selection.selectors
      .map {
        case StringSelector(criteria, operator, operatorMode, values) =>
          val column = criteriaToColumn(criteria)
          val sqlOperation = s"${operator.rep} ${operatorMode.rep}"
          val escapedValues = values.map(value => s""""$value"""")
          s"$column $sqlOperation('{${escapedValues.mkString(",")}}')"
      }.mkString(" AND ")

  private def criteriaToColumn(criteria: Criteria[_]): String =
    criteria match {
      case Criteria.Job => "name"
    }
}

private[postgres] object SqlSelectionCompiler extends SqlSelectionCompiler
