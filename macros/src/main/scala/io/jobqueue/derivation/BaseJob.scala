package io.jobqueue.derivation

import io.jobqueue.queue.{ JobDecoder, JobEncoder }

import scala.annotation.{ compileTimeOnly, StaticAnnotation }
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macro paradise to expand macro annotations")
class BaseJob extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro BaseJobImpl.impl
}

object BaseJobImpl {
  def impl(c: whitebox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    val JobEncoderClass = typeOf[JobEncoder[_]].typeSymbol.asType
    val JobDecoderClass = typeOf[JobDecoder[_]].typeSymbol.asType

    annottees match {
      case List(classDef: ClassDef) if classDef.mods.hasFlag(Flag.SEALED) =>
        val tpname = classDef.name
        val Type = tpname

        val encoderName = TermName("encode" + tpname.decodedName)
        val decoderName = TermName("decode" + tpname.decodedName)

        q"""
           $classDef

           object ${classDef.name.toTermName} {
              import _root_.io.circe.syntax._

              implicit val $encoderName: $JobEncoderClass[$Type] = _root_.com.jobsqueue.derivation.Derivator.encoder[$Type]
              implicit val $decoderName: $JobDecoderClass[$Type] = _root_.com.jobsqueue.derivation.Derivator.decoder[$Type]
           }
         """
      case _ =>
        c.abort(c.enclosingPosition, s"sorry")
    }
  }
}
