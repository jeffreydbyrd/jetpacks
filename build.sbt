name := """jetpacks"""

version := "0.0-SNAPSHOT"

libraryDependencies ++= {
  val akkaV = "2.2.3"
  Seq(
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-contrib" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV
  )
}

play.Project.playScalaSettings
