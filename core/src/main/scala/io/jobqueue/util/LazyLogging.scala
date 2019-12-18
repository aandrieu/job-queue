package io.jobqueue.util

import com.typesafe.scalalogging.Logger
import net.logstash.logback.argument.{ StructuredArgument, StructuredArguments }
import org.slf4j.LoggerFactory

private[jobqueue] class JsonLogger(logger: Logger) {
  def debug(
    message: String,
    info:    Map[String, Any] = Map.empty,
    context: Option[Context] = None
  ): Unit =
    logger.debug(message, toArguments(info, context): _*)

  def info(
    message: String,
    info:    Map[String, Any] = Map.empty,
    context: Option[Context] = None
  ): Unit =
    logger.info(message, toArguments(info, context): _*)

  def warn(
    message: String,
    info:    Map[String, Any] = Map.empty,
    context: Option[Context] = None
  ): Unit =
    logger.warn(message, toArguments(info, context): _*)

  def error(
    message:   String,
    throwable: Option[Throwable] = None,
    info:      Map[String, Any] = Map.empty,
    context:   Option[Context] = None
  ): Unit =
    logger.error(message, toArguments(info, context, throwable): _*)

  private def toArguments(
    info:      Map[String, Any] = Map.empty,
    context:   Option[Context] = None,
    throwable: Option[Throwable] = None
  ): List[StructuredArgument] = {
    val argumentsMap =
      info ++
        throwable.map("exceptionStackTrace" -> _).toMap ++
        context.map(_.toLogMap).getOrElse(Map.empty)

    argumentsMap.toList.collect {
      case (key, value) =>
        StructuredArguments.kv(key, value)
    }
  }
}

trait Logging {
  val logger: JsonLogger = new JsonLogger(
    Logger(LoggerFactory.getLogger(this.getClass))
  )
}
