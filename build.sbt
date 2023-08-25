ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "dailymeows",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.1",
      "io.github.apimorphism" %% "telegramium-core" % "8.68.0",
      "io.github.apimorphism" %% "telegramium-high" % "8.68.0",
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "org.tpolecat" %% "skunk-core" % "0.6.0",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.4"
    )
  )
