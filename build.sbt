name := """Learning AKKA"""

version := "1.0"

scalaVersion := "2.11.1"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.3"
)

