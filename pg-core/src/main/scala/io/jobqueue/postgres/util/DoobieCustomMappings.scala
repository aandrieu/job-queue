package io.jobqueue.postgres.util

import cats.implicits._
import doobie.util.Meta
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

private[postgres] object DoobieCustomMappings {
  implicit val jsonMetaMapping: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json").timap[Json](
        a => parse(a.getValue).leftMap[Json](e => throw e).merge
      )(
        a => {
          val o = new PGobject
          o.setType("json")
          o.setValue(a.noSpaces)
          o
        }
      )
}
