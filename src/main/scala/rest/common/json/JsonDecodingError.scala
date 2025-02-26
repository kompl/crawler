package com.crawler.rest.common.json

import cats.data.NonEmptyList
import com.crawler.JsonError

private[rest] final class JsonDecodingError(errors: NonEmptyList[JsonError])
    extends Exception(
      s"JSON decoding failed with com.crawler.errors: ${errors.toList.mkString(", ")}"
    )
