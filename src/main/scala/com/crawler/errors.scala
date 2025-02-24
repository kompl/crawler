package com.crawler

import java.nio.file.Path

sealed abstract class ConfigError extends Exception {
  def message: String
}

object ConfigError {
  final case class InvalidKey(path: String, error: String) extends ConfigError {
    def message: String = s"$path is missing or invalid ($error)"
  }

  final case class ParsingFailed(error: String) extends ConfigError {
    def message: String = s"Cannot parse: $error"
  }

  final case class ConfigNotFound(path: Path) extends ConfigError {
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    def message: String = s"File not found: ${path.toString}"
  }
}

sealed abstract class JsonError extends Exception {
  def message: String
}

object JsonError {
  final case class InvalidKey(path: String, error: String) extends JsonError {
    def message: String = s"${path.drop(1)} is missing or invalid ($error)"
  }

  final case class ParsingFailed(error: String) extends JsonError {
    def message: String = s"cannot parse json: $error"
  }
}

sealed abstract class HTTPClientError extends Exception {
  def message: String
}

object HTTPClientError {
  final case class InvalidUrl(url: String) extends HTTPClientError {
    def message: String = s"Invalid URL: $url"
  }

  final case class InvalidContentType(url: String) extends HTTPClientError {
    def message: String = s"URL did not return HTML: $url"
  }

  final case class ClientFailure(url: String, reason: String)
      extends HTTPClientError {
    def message: String = s"HTTP client error for $url: $reason"
  }

  final case class UnexpectedStatus(status: String) extends HTTPClientError {
    def message: String = s"Unexpected status code: $status"
  }
}

sealed abstract class HTMLStreamParserError extends Exception {
  def message: String
}

object HTMLStreamParserError {
  final case class TitleNotFound(message: String) extends HTMLStreamParserError
}
