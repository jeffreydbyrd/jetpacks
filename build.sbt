name := """jetpacks"""

organization := """com.doppelgamer"""

version := "0.0-SNAPSHOT"

libraryDependencies ++= {
  val akkaV = "2.2.3"
  Seq(
    "org.scalatest" % "scalatest_2.10" % "2.1.4" % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-contrib" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.doppelgamer" %% "doppelengine" % "0.0-SNAPSHOT"
  )
}

play.Project.playScalaSettings
