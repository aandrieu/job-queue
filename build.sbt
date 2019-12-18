val scalacFlags = Seq(
  "-Xfatal-warnings",
  "-Ymacro-annotations",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-deprecation",
  "-explaintypes",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-unused:-implicits,-explicits,-privates,-locals,_"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  scalacOptions --= Seq("-target:jvm-1.7"),
  scalacOptions ++= scalacFlags
)

lazy val root = (project in file("."))
  .aggregate(
    core,
    pg_core,
    macros,
    example
  )
  .settings(commonSettings)
  .settings(name := "job-queue")

lazy val core = (project in file("core"))
  .settings(name := "core")
  .settings(commonSettings)
  .settings(libraryDependencies := Dependencies.core)

lazy val pg_core = (project in file("pg-core"))
  .settings(name := "pg-core")
  .settings(commonSettings)
  .settings(libraryDependencies := Dependencies.pgCore)
  .dependsOn(
    core
  )

lazy val macros = (project in file("macros"))
  .settings(
    name := "macros",
    scalacOptions ++= Seq(
      "-language:experimental.macros"
    )
  )
  .settings(commonSettings)
  .settings(libraryDependencies := Dependencies.macros)
  .dependsOn(
    core
  )

lazy val example = (project in file("example"))
  .settings(name := "example")
  .settings(commonSettings)
  .settings(libraryDependencies := Dependencies.example)
  .dependsOn(
    core,
    pg_core,
    macros
  )