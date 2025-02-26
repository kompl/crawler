package com.crawler.core

import cats.{Applicative, Monoid, Parallel}
import cats.effect.*
import cats.syntax.all.*
import cats.effect.syntax.all.*
import com.crawler.common.{HTMLStreamParser, HTTPClientResource}
import com.crawler.{HTMLStreamParserError, HTTPClientError}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import fs2.Stream
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.headers.`Content-Type`

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*

class CrawlerSuite extends CatsEffectSuite {

  val methods = List(1).getClass.getMethods
  methods.foreach(println)

  def bubblePass(lst: List[Int]): (List[Int], Boolean) = {
    (lst.headOption, lst.tail) match {
      case (None, _) => (lst, false)
      case (Some(h), Nil) => (lst, false)
      case (Some(h), tail) =>
        val (swapped, last, acc) = tail.foldLeft(false, h, List.empty[Int]) {
          case ((sw, prev, acc), x) => if (prev < x) {
            (sw, x, acc :+ prev)
          } else {
            (true, prev, acc :+ x)
          }
        }

        (acc :+ last, swapped)
    }
  }

  def bubbleSort(lst: List[Int]): List[Int] = {
    (1 until lst.size).foldLeft(lst, true) {
      case ((acc, swapped), _) => if (swapped) {
        bubblePass(acc)
      } else {
        (acc, swapped)
      }
    }._1
  }

  def mep(opt: Int): Future[Int] = ???

  def trav(l: List[Int]): Future[List[Int]] = {
    l.traverse(mep)
  }

  def seqq(l: List[Option[Int]]): Option[List[Int]] = {
    l.sequence
  }

  def asd[A : Numeric, F[_] : Applicative](l: List[A]): F[A] = {
    l.sum.pure[F]
  }

  val e = List(1) ::: List(2)

  val o: Option[Int] = Some(1)


  def bubbleSortM(lst: List[Int]): List[Int] = {
    // Начальное значение аккумулятора
    val init = (lst, true)
    // Проводим сворачивание по диапазону итераций
    val result: Either[String, (List[Int], Boolean)] =
      (1 until lst.size).toList.foldM(init) { case ((acc, swapped), _) =>
        if (!swapped) Either.left("sorted")  // Раннее завершение, если обменов не было
        else Either.right(bubblePass(acc))
      }
    // Если раннее завершение произошло, возвращаем исходное значение аккумулятора
    result match {
      case Right((sorted, _)) => sorted
      case Left(_)            => lst
    }
  }

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val concurrentEffect: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit val parallel: Parallel[IO] = IO.ioParallel
  implicit val concurrent: Concurrent[IO] = IO.ioConcurrentEffect
  implicit val ecResource: Resource[IO, ExecutionContext] =
    Resource.pure(ec)
  val validUrl: String Refined Uri = Refined.unsafeApply("https://example.com")
  val invalidUrl: String Refined Uri = Refined.unsafeApply("invalid-url")
  val timeoutUrl: String Refined Uri =
    Refined.unsafeApply("https://timeout.com")

  val validResponse = Response[IO](status = Status.Ok)
    .withEntity(
      "<html><head><title>Test Title</title></head><body></body></html>"
    )
    .withContentType(`Content-Type`(MediaType.text.html))

  val invalidResponse = Response[IO](status = Status.Ok)
    .withEntity("<html><head></head><body>No title</body></html>")
    .withContentType(`Content-Type`(MediaType.text.html))

  val timeoutResponse: IO[Response[IO]] =
    IO.sleep(1.seconds) *> IO.raiseError(new RuntimeException("Timeout"))

  def mockHttpClient(response: IO[Response[IO]]): HTTPClientResource[IO] =
    new HTTPClientResource[IO] {
      def get(
          uri: org.http4s.Uri
      ): Resource[IO, Either[HTTPClientError, Response[IO]]] =
        Resource.eval(
          response.attempt.map(
            _.leftMap(e =>
              HTTPClientError.ClientFailure(uri.toString(), e.getMessage)
            )
          )
        )
    }

  def mockHtmlParser(success: Boolean): HTMLStreamParser[IO] =
    new HTMLStreamParser[IO] {
      def title(
          stream: Stream[IO, Byte]
      ): IO[Either[HTMLStreamParserError, String]] =
        if (success) IO.pure(Right("Test Title"))
        else
          IO.pure(Left(HTMLStreamParserError.TitleNotFound("Title not found")))
    }

  // ✅ **Тест на успешное извлечение заголовка**
  test("Crawler should fetch title successfully") {
    case class User(name: String, age: Int)

    val userBase = List(
      User("Travis", 28),
      User("Kelly", 33),
      User("Jennifer", 44),
      User("Dennis", 23))

    def twentySomethings =
      (for {
        user <- userBase
        if user.age >=20 && user.age < 30
  } yield (user.name -> user.age)).toMap

    def gen(xx: List[String]): List[String] =
      for {
        ch <- xx
        ch2 <- xx if ch != ch2
      } yield ch + ch2

    val xs = List(1, 2, 3)
    val comb = xs.combinations(2).toList

    val crawler = new Crawler.Impl[IO](
      mockHttpClient(IO.pure(validResponse)),
      mockHtmlParser(success = true),
      threadsCount = 4
    )

    crawler.fetchTitles(List(validUrl)).map { results =>
      assertEquals(results.length, 1)
      assertEquals(results.head.result, Crawler.Result.Success("Test Title"))
    }
  }

  // ❌ **Тест на ошибку (нет `<title>` в HTML)**
  test("Crawler should fail if no title is found") {
    val crawlerWithInvalidTitle = new Crawler.Impl[IO](
      mockHttpClient(IO.pure(invalidResponse)),
      mockHtmlParser(success = false),
      threadsCount = 4
    )

    crawlerWithInvalidTitle.fetchTitles(List(validUrl)).map { results =>
      assertEquals(results.length, 1)
      assert(results.head.result.isInstanceOf[Crawler.Result.Failure])
    }
  }

  // ⏳ **Тест на ошибку таймаута**
  test("Crawler should fail on timeout") {
    val crawlerWithTimeout = new Crawler.Impl[IO](
      mockHttpClient(timeoutResponse),
      mockHtmlParser(success = true),
      threadsCount = 4
    )

    val resultOpt =
      crawlerWithTimeout.fetchTitles(List(timeoutUrl)).unsafeRunTimed(3.seconds)

    resultOpt match {
      case Some(results) =>
        assert(results.head.result.isInstanceOf[Crawler.Result.Failure])
        assert(
          results.head.result
            .asInstanceOf[Crawler.Result.Failure]
            .error
            .contains("Timeout")
        )
      case None =>
        fail("Timeout error was expected but the test timed out instead")
    }
  }
}
