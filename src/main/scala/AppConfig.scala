package com.crawler

import cats.data.{Validated, ValidatedNel}
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{LessEqual, NonNegative}
import eu.timepit.refined.string.IPv4
import eu.timepit.refined.types.all.{NonEmptyString, PosInt}
import io.circe.*
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.*
import io.circe.yaml.parser.parse
import io.circe.refined.*

import java.io.{FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths}

final case class AppConfig(
    threadsPoolSize: PosInt,
    api: AppConfig.Api
)

object AppConfig {
  final case class Api(
      host: Host,
      port: Port,
      secretKey: Option[NonEmptyString]
  )

  final case class RawConfig(
      threadsPoolSize: PosInt,
      api: RawConfig.Api
  )

  object RawConfig {

    implicit val customConfig: Configuration =
      Configuration.default.withSnakeCaseMemberNames.withDefaults

    final case class Api(
        host: Host,
        port: Port,
        secret: Option[NonEmptyString]
    )

    implicit val rawConfigApiDecoder: Decoder[RawConfig.Api] =
      deriveConfiguredDecoder[RawConfig.Api]

    implicit val rawConfigDecoder: Decoder[AppConfig.RawConfig] =
      deriveConfiguredDecoder[AppConfig.RawConfig]
  }

  private type Port = Int Refined (NonNegative And LessEqual[65535])
  private type Host = String Refined IPv4

  def load[F[_]: Sync]: F[ValidatedNel[ConfigError, AppConfig]] = {
    val path = Paths.get("config", "main.yml").toAbsolutePath

    fromFile[F](path)
      .map(_.map { c =>
        AppConfig(
          threadsPoolSize = c.threadsPoolSize,
          api = Api(
            host = c.api.host,
            port = c.api.port,
            secretKey = c.api.secret
          )
        )
      })
  }

  private def fromFile[F[_]: Sync](
      path: Path
  ): F[ValidatedNel[ConfigError, RawConfig]] =
    Sync[F]
      .delay(Files.exists(path))
      .flatMap {
        case true =>
          Sync[F]
            .delay(
              new FileInputStream(path.toFile)
            )
            .flatMap(fromStream[F])
        case false =>
          Sync[F].pure(Validated.invalidNel(ConfigError.ConfigNotFound(path)))
      }

  private def fromStream[F[_]: Sync](
      input: InputStream
  ): F[ValidatedNel[ConfigError, RawConfig]] =
    for {
      string <-
        Sync[F]
          .delay(input.readAllBytes())
          .map(bytes => new String(bytes, "UTF-8"))
    } yield fromString[F](string)

  private def fromString[F[_]: Sync](
      input: String
  ): ValidatedNel[ConfigError, RawConfig] = {
    parse(input) match {
      case Left(parsingFailure) =>
        Validated.invalidNel(ConfigError.ParsingFailed(parsingFailure.message))
      case Right(json) =>
        Decoder[RawConfig]
          .decodeAccumulating(json.hcursor)
          .leftMap { decodingFailures =>
            decodingFailures.map { df =>
              ConfigError.InvalidKey(CursorOp.opsToPath(df.history), df.message)
            }
          }
    }
  }
}
