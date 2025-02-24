package com.crawler.rest.common.error_handling

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import com.crawler.rest.common.json.JsonDecodingError
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder, Response}

trait HttpErrorHandler[F[_], E <: Throwable] extends Http4sDsl[F] {
  def handle(routes: AuthedRoutes[Unit, F]): AuthedRoutes[Unit, F]

  implicit val ME: MonadError[F, E]

  implicit def jsonEncoder: EntityEncoder[F, Json] = jsonEncoderOf[F, Json]

  def unknownErrorHandler: Throwable => F[Response[F]] =
    e =>
      InternalServerError(
        Json.obj(
          "code" -> "UnknownError".asJson,
          "message" -> e.getMessage.asJson
        )
      )

  def decodingErrorHandler: JsonDecodingError => F[Response[F]] =
    e =>
      BadRequest(
        Json.obj(
          "code" -> "DecodingError".asJson,
          "message" -> e.getMessage.asJson
        )
      )
}

abstract class RoutesHttpErrorHandler[F[_], E <: Throwable]
    extends HttpErrorHandler[F, E] {

  private def handler: E => F[Response[F]] = {
    case e: JsonDecodingError =>
      decodingErrorHandler(e)
    case e =>
      unknownErrorHandler(e)
  }

  def handle(routes: AuthedRoutes[Unit, F]): AuthedRoutes[Unit, F] =
    Kleisli { req =>
      OptionT {
        ME.handleErrorWith(routes.run(req).value)(e =>
          ME.map(handler(e))(Option(_))
        )
      }
    }
}

object HttpErrorHandler {
  def apply[F[_], E <: Throwable](implicit
      ev: HttpErrorHandler[F, E]
  ): HttpErrorHandler[F, E] = ev
}
