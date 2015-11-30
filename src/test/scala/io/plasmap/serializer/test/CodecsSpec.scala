package io.plasmap.serializer.test

import io.plasmap.model._
import io.plasmap.model.geometry.Geometry
import io.plasmap.serializer.{Codecs, GeoJsonSerialiser}
import io.plasmap.model.geometry._
import org.specs2.ScalaCheck
import org.specs2.matcher.StringMatchers
import org.specs2.mutable.Specification
import argonaut._, Argonaut._
import scodec.Attempt.Successful
import scodec.Codec
import scodec.bits.BitVector
import shapeless.Lazy

class CodecsSpec extends Specification with ScalaCheck with StringMatchers {
  import Codecs._

  def roundtrip[A](a:A)(implicit ev: Lazy[Codec[A]]) = {
    Codec[A].decode(Codec[A].encode(a).require).require.value
  }

  "Roundtrips" should {
    "work for OsmVersion" in {
      val v = OsmVersion()
      roundtrip(v) must_== v
    }

    "work forOsmNode" in {
      val n = OsmNode(
        OsmId(12L),
        Some(OsmUser("eddybaby", 3820302898L)),
        OsmVersion(),
        List("key" -> "value", "key2" -> "value2").map((OsmTag.apply _).tupled),
        HashPoint(12L)
      )
      roundtrip(n) must_== n
    }

    "work forOsmWay" in {
      val w = OsmWay(
        OsmId(12L),
        Some(OsmUser("eddybaby", 3820302898L)),
        OsmVersion(),
        List("key" -> "value", "key2" -> "value2").map((OsmTag.apply _).tupled),
        List(OsmId(12), OsmId(88))
      )
      roundtrip(w) must_== w
    }

    "work forOsmRelation" in {
      val r = OsmRelation(
        OsmId(12L),
        Some(OsmUser("eddybaby", 3820302898L)),
        OsmVersion(),
        List("key" -> "value", "key2" -> "value2").map((OsmTag.apply _).tupled),
        List(
          OsmMember(OsmTypeRelation, OsmId(121), OsmRoleOuter),
          OsmMember(OsmTypeWay, OsmId(121), OsmRoleOther("boohoo")),
          OsmMember(OsmTypeNode, OsmId(121), OsmRoleEmpty)
        )
      )
      roundtrip(r) must_== r
    }
  }
}
