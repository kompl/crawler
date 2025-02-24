package com.crawler.rest

import cats.effect.IO
import com.crawler.core.Crawler
import com.crawler.rest.common.auth.Middleware
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string
import io.circe.Json
import io.circe.literal.JsonStringContext
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets

object FakeCrawlerInstance extends FakeCrawler[IO] {
  def fetchTitles(urls: List[String Refined Uri]): IO[List[Crawler.TitleFetchingResult]] = {
    val results = urls.map { url =>
      if (url.value.contains("valid")) {
        Crawler.TitleFetchingResult(url.value, Crawler.Result.Success("Valid Title"))
      } else {
        Crawler.TitleFetchingResult(url.value, Crawler.Result.Failure("Invalid URL"))
      }
    }
    IO.pure(results)
  }
}

class V1RouterSuite extends AnyFunSuite with Matchers {
  val mockSecurity: Security[IO] = new Security[IO] {
    override def isInternalApiTokenValid: Request[IO] => IO[Boolean] = _ => IO.pure(true)
  }


  val mockCrawler: Crawler[IO] = new FakeCrawler[IO] {
    def fetchTitles(urls: List[String Refined Uri]): IO[List[Crawler.TitleFetchingResult]] = {
      val results = urls.map { url =>
        if (url.value.contains("valid")) {
          Crawler.TitleFetchingResult(url.value, Crawler.Result.Success("Valid Title"))
        } else {
          Crawler.TitleFetchingResult(url.value, Crawler.Result.Failure("Invalid URL"))
        }
      }
      IO.pure(results)
    }


    val router: HttpRoutes[IO] = new v1.Router[IO](Middleware(mockSecurity.isInternalApiTokenValid), FakeCrawlerInstance).routes

    // Вспомогательный метод для выполнения запросов
    private def runRequest(request: Request[IO]): Response[IO] = {
      router.orNotFound.run(request).unsafeRunSync()
    }

    test("POST /fetchTitles with valid URLs should return 200 OK with titles") {
      val validUrls = List("https://valid-url.com", "https://another-valid-url.com")
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
}
