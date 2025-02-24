package com.crawler
import com.typesafe.scalalogging.{Logger => ScalaLogger}
import cats.effect.Sync

class Logger[F[_]: Sync](tag: String) {
  private val logger = ScalaLogger(tag)

  def info(message: String): F[Unit] = Sync[F].delay(logger.info(message))
  def error(message: String): F[Unit] = Sync[F].delay(logger.error(message))
  def debug(message: String): F[Unit] = Sync[F].delay(logger.debug(message))
  def warn(message: String): F[Unit] = Sync[F].delay(logger.warn(message))
}
