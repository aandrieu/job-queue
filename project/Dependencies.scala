import sbt._

object Dependencies {

  val akkaTypedVersion = "2.6.0"
  val catsEffectVersion = "2.0.0"
  val circeVersion = "0.12.3"
  val configVersion = "1.4.0"
  val doobieVersion = "0.8.6"
  val logbackVersion = "1.2.3"
  val logbackEncoderVersion = "5.2"
  val macrosVersion = "2.13.1"
  val postgresVersion = "42.2.8"
  val scalaLoggingVersion = "3.9.2"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaTypedVersion
  )

  lazy val catsEffect = Seq(
    "org.typelevel" %% "cats-effect" % catsEffectVersion
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core"    % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser"  % circeVersion
  )

  lazy val config = Seq(
    "com.typesafe" % "config" % configVersion
  )

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion
  )

  lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"           % scalaLoggingVersion,
    "ch.qos.logback"             % "logback-core"             % logbackVersion,
    "ch.qos.logback"             % "logback-classic"          % logbackVersion,
    "net.logstash.logback"       % "logstash-logback-encoder" % logbackEncoderVersion
  )

  lazy val postgres = Seq(
    "org.postgresql" % "postgresql" % postgresVersion
  )

  lazy val common = circe ++ catsEffect ++ config ++ logging

  lazy val core = common ++ akka

  lazy val pgCore = common ++ postgres ++ doobie

  lazy val example = common ++ postgres

  lazy val macros = Seq(
    "org.scala-lang" % "scala-reflect"  % macrosVersion,
    "org.scala-lang" % "scala-compiler" % macrosVersion % Provided
  )
}
