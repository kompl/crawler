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
    val doobie = "0.13.4"
    object enumeratum {
      val core = "1.7.0"
      val circe = "1.7.0"
      val doobie = "1.7.0"
    }
    val fs2 = "2.5.10"
    val http4s = "0.22.1"
    val kindProjector = "0.13.2"
    val logback = "1.2.3"
    val logging = "3.9.2"
    val monix = "3.4.0"
    val refined = "0.9.27"
    val ojdbc = "19.9.0.0"
    val aqapi = "19.3.0.0"
    val jms = "2.0.1"
    val transactionApi = "1.3"
    val cats = "2.6.1"
    val catsEffect = "2.5.4"
    val pprint = "0.6.6"
    val fs2Kafka = "1.10.0"
    val vulcan = "1.8.0"
    val awsSDK = "2.17.188"
    val munit = "1.0.7"
  }

  val bm4 = "com.olegpy" %% "better-monadic-for" % version.bm4

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

  val diffx = "com.softwaremill.diffx" %% "diffx-scalatest" % version.diffx

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core" % version.doobie,
    "org.tpolecat" %% "doobie-hikari" % version.doobie,
    "org.tpolecat" %% "doobie-refined" % version.doobie,
    "org.tpolecat" %% "doobie-scalatest" % version.doobie
  )

  val enumeratum = Seq(
    "com.beachape" %% "enumeratum" % version.enumeratum.core,
    "com.beachape" %% "enumeratum-circe" % version.enumeratum.circe,
    "com.beachape" %% "enumeratum-doobie" % version.enumeratum.doobie
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

  val monix = Seq(
    "io.monix" %% "monix" % version.monix
  )

  val refined = Seq(
    "eu.timepit" %% "refined" % version.refined
  )

  val ojdbc = Seq(
    "com.oracle.database.jdbc" % "ojdbc10" % version.ojdbc
  )

  val aqapi = Seq(
    "com.oracle.database.messaging" % "aqapi" % version.aqapi
  )

  val jms = Seq(
    "javax.jms" % "javax.jms-api" % version.jms
  )

  val transactionApi = Seq(
    "javax.transaction" % "javax.transaction-api" % version.transactionApi
  )

  val pprint = "com.lihaoyi" %% "pprint" % version.pprint

  val vulcan = Seq(
    "com.github.fd4s" %% "vulcan" % version.vulcan,
    "com.github.fd4s" %% "vulcan-generic" % version.vulcan
  )

  val fs2Kafka = Seq(
    "com.github.fd4s" %% "fs2-kafka" % version.fs2Kafka,
    "com.github.fd4s" %% "fs2-kafka-vulcan" % version.fs2Kafka
  )

  val awsSdk = Seq(
    "software.amazon.awssdk" % "s3" % version.awsSDK
  )
}
