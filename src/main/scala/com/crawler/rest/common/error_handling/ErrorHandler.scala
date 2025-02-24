package com.crawler.rest.common.error_handling

import cats.MonadError

object ErrorHandler {
  def apply[F[_]: MonadError[*[_], Throwable]]: HttpErrorHandler[F, Throwable] =
    new RoutesHttpErrorHandler[F, Throwable] {
      val ME: MonadError[F, Throwable] = implicitly
    }
}
