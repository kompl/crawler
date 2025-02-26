package com.crawler.rest

import cats.effect.IO
import com.crawler.core.Crawler
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string

class FakeCrawler[F[_]] extends Crawler[F] {
  override def fetchTitles(
      urls: List[Refined[String, string.Uri]]
  ): F[List[Crawler.TitleFetchingResult]] = {
    // Пример реализации, возвращающий заглушку
    val results = urls.map { url =>
      if (url.value.contains("valid"))
        Crawler.TitleFetchingResult(
          url.value,
          Crawler.Result.Success("Valid Title")
        )
      else
        Crawler.TitleFetchingResult(
          url.value,
          Crawler.Result.Failure("Invalid URL")
        )
    }
    // Предполагается, что F — это IO, либо нужно использовать соответствующий синтаксис для оборачивания в F
    cats.effect.IO
      .pure(results)
      .asInstanceOf[F[List[Crawler.TitleFetchingResult]]]
  }

  override def streamTitles(
      urls: List[Refined[String, string.Uri]]
  ): fs2.Stream[F, Crawler.TitleFetchingResult] = ???
}

object FakeCrawlerInstance extends FakeCrawler[IO] {}
