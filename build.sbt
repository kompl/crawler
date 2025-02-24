import Dependencies._

lazy val root = (project in file(".")).settings(
    inThisBuild(
        List(
            organization := "com.crawler",
            scalaVersion := "2.13.12",
            version := "0.1.0-SNAPSHOT"
        )
    ),
    name := "crawler",
    run / fork := true,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += diffx % Test,
    libraryDependencies += pprint % Test,
    libraryDependencies ++= munit,
    libraryDependencies += cats,
    libraryDependencies += catsEffect,
    libraryDependencies ++= circe,
    libraryDependencies ++= fs2,
    libraryDependencies ++= http4s,
    libraryDependencies ++= logback,
    libraryDependencies ++= logging,
    libraryDependencies ++= refined,
  libraryDependencies += "org.jsoup" % "jsoup" % "1.17.2",
    wartremoverErrors ++= Warts.allBut(
        Wart.Any,
        Wart.Nothing,
        // useful for debugging and type inference
        Wart.TripleQuestionMark
    ),
    scalacOptions ++= Seq(
        "-Xfatal-warnings",
        "-Xsource:3",
        "-unchecked",
        "-deprecation",
        "-P:kind-projector:underscore-placeholders",
        s"-P:wartremover:excluded:${(Test / scalaSource).value}"
    ),
    Compile / console / scalacOptions :=
      (console / scalacOptions).value.filterNot(s =>
          s.contains("wartremover") || s.contains("fatal-warnings")
      ),
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
    assembly / assemblyMergeStrategy := {
        case PathList("module-info.class")                 => MergeStrategy.discard
        case PathList("scala", "collection", "compat", _*) => MergeStrategy.first
        case PathList("scala", "util", _*)                 => MergeStrategy.first
        case x if x.endsWith("scala-collection-compat.properties") =>
            MergeStrategy.first
        case x =>
            val oldStrategy = (assembly / assemblyMergeStrategy).value
            oldStrategy(x)
    }
)

addCompilerPlugin(bm4)
addCompilerPlugin(kindProjector cross CrossVersion.full)
