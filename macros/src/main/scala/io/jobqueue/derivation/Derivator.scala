package io.jobqueue.derivation

import io.jobqueue.job.{ Job, JsonJob }
import io.jobqueue.queue.{ JobDecoder, JobEncoder }

import scala.collection.immutable
import scala.reflect.macros.whitebox

object DeriveJobEncoder {
  def apply[A <: Job]: JobEncoder[A] = macro DerivatorImpl.encoder[A]
}

object DeriveJobDecoder {
  def apply[A <: Job]: JobDecoder[A] = macro DerivatorImpl.decoder[A]
}

object JobsNameFor {
  def apply[A <: Job]: immutable.List[String] = macro DerivatorImpl.jobsNameFor[A]
}

class DerivatorImpl(val c: whitebox.Context) {
  import c.universe._

  private val JobEncoderClass = typeOf[JobEncoder[_]].typeSymbol.asClass
  private val JobDecoderClass = typeOf[JobDecoder[_]].typeSymbol.asClass
  private val JobDefClass = typeOf[JsonJob].typeSymbol.companion
  private val JobDefType = typeOf[JsonJob].typeSymbol.asType
  private val List = typeOf[immutable.List[String]].typeSymbol.companion

  private def listSubclasses[A: c.WeakTypeTag]: Set[Symbol] =
    weakTypeOf[A].typeSymbol.asClass.knownDirectSubclasses.flatMap { subClass =>
      val Class = subClass.asClass
      if (Class.isTrait && Class.isSealed) {
        Class.knownDirectSubclasses
      } else if (Class.isCaseClass && Class.isFinal) {
        Set(subClass)
      } else {
        Set.empty
      }
    }

  private def symbolToJobName(symbol: Symbol): String =
    symbol.name.toString

  def jobsNameFor[A: c.WeakTypeTag]: c.Tree = {
    val args = listSubclasses.map(symbolToJobName)
    q"""
       $List(..$args)
     """
  }

  def encoder[A: c.WeakTypeTag]: c.Tree = {
    val A = weakTypeOf[A]

    val cases: Set[c.universe.Tree] = listSubclasses.map { subClass =>
      val fullName = symbolToJobName(subClass)
      val termName = TermName(fullName.toLowerCase)
      val Type = subClass.asType.name
      cq"""$termName: $Type => $JobDefClass($fullName, $termName.asJson)"""
    }

    q"""
    new $JobEncoderClass[$A] {
      import _root_.io.circe.syntax._

      override def encode(job: $A): $JobDefType = job match{
        case ..$cases
        case _ => throw new Exception(s"Failed to encode type '$${job.toString}'!")
      }
    }
    """
  }

  def decoder[A: c.WeakTypeTag]: c.Tree = {
    val A = weakTypeOf[A]

    val cases: Set[c.universe.Tree] = listSubclasses.map { subClass =>
      val fullName = symbolToJobName(subClass)
      val Type = subClass.asType.name
      cq"""$JobDefClass($fullName, params) => params.as[$Type].getOrElse(
          throw new RuntimeException(s"Failed to decode type '$${jobDef.toString}'!")
      )"""
    }

    q"""
    new $JobDecoderClass[$A] {
      import _root_.io.circe.syntax._

      override def decode(jobDef: $JobDefType): $A = jobDef match {
        case ..$cases
        case _ => throw new Exception(s"Failed to decode type '$${jobDef.toString}'!")
      }
    }
    """
  }
}
