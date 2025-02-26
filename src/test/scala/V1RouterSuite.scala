package com.crawler.rest

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import com.crawler.rest.common.auth.Middleware
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class V1RouterSuite extends AnyFunSuite with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val concurrent: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val mockSecurity: Security[IO] = new Security[IO] {
    override def isInternalApiTokenValid: Request[IO] => IO[Boolean] =
      _ => IO.pure(true)
  }

  val router: HttpRoutes[IO] = new v1.Router[IO](
    Middleware(mockSecurity.isInternalApiTokenValid),
    FakeCrawlerInstance
  ).routes

  // Вспомогательный метод для выполнения запросов
  private def runRequest(request: Request[IO]): Response[IO] = {
    router.orNotFound.run(request).unsafeRunSync()
  }

  test("POST /fetchTitles with valid URLs should return 200 OK with titles") {
    val validUrls =
      List("https://valid-url.com", "https://another-valid-url.com")
    val jsonBody = Json.obj("sites" -> validUrls.asJson)

    val request = POST(jsonBody, uri"/fetchTitles")

    val response = runRequest(request)

    response.status shouldBe Status.Ok
    val expectedResponse = Json.arr(
      Json.obj(
        "url" -> Json.fromString("https://valid-url.com"),
        "result" -> Json.obj(
          "title" -> Json.fromString("Valid Title"),
          "type" -> Json.fromString("Success")
        )
      ),
      Json.obj(
        "url" -> Json.fromString("https://another-valid-url.com"),
        "result" -> Json.obj(
          "title" -> Json.fromString("Valid Title"),
          "type" -> Json.fromString("Success")
        )
      )
    )

    response.as[Json].unsafeRunSync() shouldBe expectedResponse
  }

  test("POST /fetchTitles with invalid URLs should return 200 OK with errors") {
    val invalidUrls = List("https://some-url.com", "https://another-url.com")
    val jsonBody = Json.obj("sites" -> invalidUrls.asJson)

    val request = POST(jsonBody, uri"/fetchTitles")

    val response = runRequest(request)

    response.status shouldBe Status.Ok
    val expectedResponse = Json.arr(
      Json.obj(
        "url" -> Json.fromString("https://some-url.com"),
        "result" -> Json.obj(
          "error" -> Json.fromString("Invalid URL"),
          "type" -> Json.fromString("Failure")
        )
      ),
      Json.obj(
        "url" -> Json.fromString("https://another-url.com"),
        "result" -> Json.obj(
          "error" -> Json.fromString("Invalid URL"),
          "type" -> Json.fromString("Failure")
        )
      )
    )

    response.as[Json].unsafeRunSync() shouldBe expectedResponse
  }
}
