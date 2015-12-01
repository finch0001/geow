package io.plasmap.serializer

import io.plasmap.model.geometry._
import io.plasmap.model._
import scodec._
import scodec.bits.BitVector
import scodec.codecs._
import scodec.codecs.literals._

/**
  * Created by mark on 30.11.15.
  */
object Codecs {

  implicit val osmVersionCodec:Codec[OsmVersion] = (
      ("timestamp"     | int64) ::
      ("versionNumber" | uint16) ::
      ("changeset"     | int32) ::
      ("visible"       | bool)
    ).as[OsmVersion]

  implicit val osmIdCodec:Codec[OsmId] =
    ("value" | int64).as[OsmId]

  implicit val osmUserCodec:Codec[OsmUser] = (
    ("username" | utf8_32 ) ::
    ("uid"      | int64)
  ).as[OsmUser]

  implicit val osmTagCodec:Codec[OsmTag] = (
    ("key"   | utf8_32) ::
    ("value" | utf8_32)
  ).as[OsmTag]

  implicit val geometryHashPointCodec:Codec[HashPoint] =
    ("hash" | int64).as[HashPoint]

  implicit val geometryLonLatPointCodec:Codec[LonLatPoint] = (
    ("lon" | double) ::
    ("lat" | double)
  ).as[LonLatPoint]

  import scodec.bits._


  implicit val osmTypeCodec:Codec[OsmType] = mappedEnum(
    uint2, OsmTypeNode -> 0, OsmTypeWay -> 1, OsmTypeRelation -> 2
  )

  //OSM Roles
  implicit val fixedOsmRolesCodec:Codec[OsmRole] = mappedEnum(
    uint2,
    OsmRoleEmpty     -> 0,
    OsmRoleInner     -> 1,
    OsmRoleOuter     -> 2
  )
  implicit val roleOtherCodec:Codec[OsmRoleOther] = ("value" | utf8_32).as[OsmRoleOther]

  implicit val osmRoleCodec:Codec[OsmRole] = discriminatorFallback(
    (constant(bin"11") ~> utf8_32).as[OsmRoleOther], fixedOsmRolesCodec)
    .xmapc(_.fold(identity, identity)){
      case o:OsmRoleOther => Left(o);
      case r => Right(r)
    }

  implicit val osmMemberCodec:Codec[OsmMember] = (
      ("typ"  | osmTypeCodec) ::
      ("ref"  | osmIdCodec)   ::
      ("role" | osmRoleCodec)
    ).as[OsmMember]


  implicit val geometryPointCodec:Codec[Point] =
    Codec.coproduct[Point].choice //TODO: Maybe not choice

  implicit val geometryLineStringCodec:Codec[LineString] =
    ("coordinates" | listOfN(int32, double ~ double))
      .as[LineString]

  implicit def geometryCodec:Codec[Geometry] = lazily {
    discriminated[Geometry].by(uint4)
      .| (0) { case GeometryCollection(geos) =>  geos } (GeometryCollection.apply) (list(geometryCodec))
      .| (1) { case LonLatPoint(lon, lat)   => (lon, lat) } { case (lon, lat) => LonLatPoint(lon, lat)} (double ~ double)
      .| (2) { case HashPoint(hash)         => hash   }  (HashPoint.apply)         (int64)
      .| (3) { case MultiPoint(coords)      => coords }  (MultiPoint.apply)        (list(double ~ double))
      .| (4) { case LineString(coords)      => coords }  (LineString.apply)        (list(double ~ double))
      .| (5) { case MultiLineString(coords) => coords }  (MultiLineString.apply)   (list(list(double ~ double)))
      .| (6) { case Polygon(coords)         => coords }  (Polygon.apply)           (list(list(double ~ double)))
      .| (7) { case MultiPolygon(coords)    => coords }  (MultiPolygon.apply)      (list(list(list(double ~ double))))
  }

  implicit val osmNodeCodec:Codec[OsmNode] = (
      ("id"         | osmIdCodec                  ) ::
      ("user"       | optional(bool, osmUserCodec)) ::
      ("version"    | osmVersionCodec             ) ::
      ("tags"       | listOfN(int8, osmTagCodec)  ) ::
      ("point"      | geometryPointCodec)
    ).as[OsmNode]

  implicit val osmWayCodec:Codec[OsmWay] = (
      ("id"         | osmIdCodec                  ) ::
      ("user"       | optional(bool, osmUserCodec)) ::
      ("version"    | osmVersionCodec             ) ::
      ("tags"       | listOfN(int8, osmTagCodec)  ) ::
      ("nds"        | listOfN(int32, osmIdCodec)  )
    ).as[OsmWay]

  implicit val osmRelationCodec:Codec[OsmRelation] = (
      ("id"         | osmIdCodec                  ) ::
      ("user"       | optional(bool, osmUserCodec)) ::
      ("version"    | osmVersionCodec             ) ::
      ("tags"       | listOfN(int8, osmTagCodec)  ) ::
      ("refs"       | listOfN(int32, osmMemberCodec)  )
    ).as[OsmRelation]
}
