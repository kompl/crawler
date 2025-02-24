package com.crawler.common

import cats.effect.{Async, ConcurrentEffect, Resource, Timer}
import cats.syntax.all.*
import com.crawler.HTTPClientError
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.{FollowRedirect, Logger}
import org.http4s.{Request, Response, Uri => Http4sUri}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

trait HTTPClientResource[F[_]] {
  def get(uri: Http4sUri): Resource[F, Either[HTTPClientError, Response[F]]]
}

object HTTPClientResource {
  final class Impl[F[_]: Async: ConcurrentEffect: Timer](client: Client[F])
      extends HTTPClientResource[F]
      with Http4sClientDsl[F] {
    private val httpClient: Client[F] = Logger[F](
      logHeaders = true,
      logBody = false
    )(client)

    private val clientWithRedirects = FollowRedirect(10)(httpClient)

    private val finalHttpClient: Client[F] = clientWithRedirects

    def get(
        uri: Http4sUri
    ): Resource[F, Either[HTTPClientError, Response[F]]] = {
      val request = Request[F](org.http4s.Method.GET, uri)

      finalHttpClient
        .run(request)
        .attempt
        .map(_.leftMap(handleClientError(uri, _)))
    }

    private def handleClientError(
        url: Http4sUri,
        e: Throwable
    ): HTTPClientError = {
      HTTPClientError
        .ClientFailure(url.toString, e.getMessage)

    }
  }
  object Impl {
    def make[F[_]: Async: ConcurrentEffect: Timer](implicit
        ecResource: Resource[F, ExecutionContext]
    ): Resource[F, HTTPClientResource[F]] = {
      ecResource.flatMap { ec =>
        BlazeClientBuilder[F](ec).resource.map(new Impl(_))
      }
    }
  }
}
