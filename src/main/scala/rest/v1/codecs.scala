package com.crawler.rest.v1

import cats.Applicative
import com.crawler.core.Crawler
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.*
import io.circe.{Decoder, Encoder}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.refined.*
private[v1] object codecs extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]
      : EntityEncoder[F, A] =
    jsonEncoderOf[F, A]
}

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri

final case class ExtractTitlesPost(
    sites: List[String Refined Uri]
)

private[v1] trait JsonCodecs {
  implicit val config: Configuration =
    Configuration.default.withDiscriminator("type")

  implicit val urlsDecoder: Decoder[ExtractTitlesPost] = deriveConfiguredDecoder

  implicit val successEncoder: Encoder[Crawler.Result.Success] =
    deriveConfiguredEncoder
  implicit val failureEncoder: Encoder[Crawler.Result.Failure] =
    deriveConfiguredEncoder
  implicit val resultEncoder: Encoder[Crawler.Result] =
    deriveConfiguredEncoder
  implicit val titleFetchingResultEncoder
      : Encoder[Crawler.TitleFetchingResult] = deriveConfiguredEncoder
}
