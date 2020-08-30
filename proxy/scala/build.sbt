name := """play-reverse"""
organization := "io.github.okanon"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.11"


libraryDependencies ++= Seq(
   guice,
   ws,
)
