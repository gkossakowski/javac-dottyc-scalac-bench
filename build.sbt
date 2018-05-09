import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "javac-dottyc-scalac-bench",
    libraryDependencies += scalaTest % Test,
    // https://mvnrepository.com/artifact/com.github.pathikrit/better-files
    libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.4.0",
  )
