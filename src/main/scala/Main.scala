import cats.data.{NonEmptyList, Validated}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.crawler.common.HTTPClientResource
import com.crawler.common.HTMLStreamParser
import com.crawler.{AppConfig, ConfigError, Logger}
import com.crawler.core.Crawler
import com.crawler.rest.*
import org.http4s.blaze.server.BlazeServerBuilder
import cats.syntax.functor.*
import eu.timepit.refined.types.all.PosInt
import org.http4s.HttpApp

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Main extends IOApp {
  type F[A] = IO[A]

  implicit val mainLogger: Logger[F] = new Logger[F]("main")

  def run(args: List[String]): F[ExitCode] = {
    AppConfig.load[F].flatMap {
      case Validated.Valid(config) =>
        implicit val ecResource: Resource[F, ExecutionContext] =
          threadPoolResource(config.threadsPoolSize)

        HTTPClientResource.Impl
          .make[F]
          .use[F, ExitCode] { httpClient =>
            val htmlParser = HTMLStreamParser.Impl.make[F]
            val security = Security.Impl.make[F](config.api)

            val algebra = Crawler.Impl
              .make[F](httpClient, htmlParser, config.threadsPoolSize.value)

            val httpApi = HttpApi.Impl.make[F](security, algebra)

            serverResource(config.api, httpApi.app, ecResource)
              .use { _ => IO.never }
              .as(ExitCode.Success)
          }
      case Validated.Invalid(errors) => logConfigErrors(errors)
    }
  }

  private def threadPoolResource(
      poolSize: PosInt
  ): Resource[F, ExecutionContext] =
    Resource
      .make(IO(Executors.newFixedThreadPool(poolSize.value))) {
        executorService => IO(executorService.shutdown())
      }
      .map(ExecutionContext.fromExecutorService)

  private def serverResource(
      config: AppConfig.Api,
      httpApp: HttpApp[IO],
      ecResource: Resource[F, ExecutionContext]
  ): Resource[IO, Unit] =
    ecResource.flatMap(
      BlazeServerBuilder[IO](_)
        .bindHttp(config.port.value, config.host.value)
        .withHttpApp(httpApp)
        .resource
        .void
    )

  private def logConfigErrors(errors: NonEmptyList[ConfigError]): F[ExitCode] =
    mainLogger.error(
      s"Cannot load config: ${errors.map(_.message).toList.mkString(", ")}"
    ) *> IO.pure(ExitCode.Error)
}
