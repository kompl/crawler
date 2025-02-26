import sbt.*

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15"

  private[Dependencies] object version {
    val bm4 = "0.3.1"
    object circe {
      val core = "0.14.1"
      val yaml = "0.14.0"
    }

    val diffx = "0.4.0"
    val fs2 = "2.5.10"
    val http4s = "0.22.1"
    val kindProjector = "0.13.2"
    val logback = "1.2.3"
    val logging = "3.9.2"
    val refined = "0.9.27"
    val cats = "2.6.1"
    val catsEffect = "2.5.4"
    val pprint = "0.6.6"
    val munit = "1.0.7"
  }
  val bm4 = "com.olegpy" %% "better-monadic-for" % version.bm4

  val diffx = "com.softwaremill.diffx" %% "diffx-scalatest" % version.diffx

  val cats = "org.typelevel" %% "cats-core" % version.cats

  val catsEffect = "org.typelevel" %% "cats-effect" % version.catsEffect

  val circe = Seq(
    "io.circe" %% "circe-core" % version.circe.core,
    "io.circe" %% "circe-generic" % version.circe.core,
    "io.circe" %% "circe-generic-extras" % version.circe.core,
    "io.circe" %% "circe-literal" % version.circe.core,
    "io.circe" %% "circe-parser" % version.circe.core,
    "io.circe" %% "circe-refined" % version.circe.core,
    "io.circe" %% "circe-yaml" % version.circe.yaml
  )

  val munit = Seq(
    "org.typelevel" %% "munit-cats-effect-2" % version.munit % Test,
    "org.http4s" %% "http4s-dsl" % version.http4s,
    "org.http4s" %% "http4s-circe" % version.http4s,
    "org.http4s" %% "http4s-client" % version.http4s
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-client" % version.http4s,
    "org.http4s" %% "http4s-dsl" % version.http4s,
    "org.http4s" %% "http4s-blaze-server" % version.http4s,
    "org.http4s" %% "http4s-blaze-client" % version.http4s,
    "org.http4s" %% "http4s-circe" % version.http4s
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % version.fs2
  )

  val kindProjector =
    "org.typelevel" %% "kind-projector" % version.kindProjector

  val logback = Seq(
    "ch.qos.logback" % "logback-classic" % version.logback
  )

  val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % version.logging
  )

  val refined = Seq(
    "eu.timepit" %% "refined" % version.refined
  )

  val pprint = "com.lihaoyi" %% "pprint" % version.pprint
}
