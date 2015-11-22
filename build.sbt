import AssemblyKeys._

name := "ganesa"

version := "1.0"

scalaVersion := "2.11.6"

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))

scalacOptions ++= (
  "-language:postfixOps" ::
    "-language:implicitConversions" ::
    "-language:higherKinds" ::
    "-language:existentials" ::
    "-deprecation" ::
    "-unchecked" ::
    "-Xlint" ::
    "-Ywarn-unused-import" ::
    "-Ywarn-unused" ::
    Nil
  )

resolvers += Opts.resolver.sonatypeReleases

resolvers += Resolver.url("typesafe", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

libraryDependencies ++= (
    ("org.scalafx" % "scalafx_2.11" % "8.0.40-R8") ::
    ("org.scalafx" %% "scalafxml-core-sfx8" % "0.2.2") ::
    ("org.apache.httpcomponents" % "httpclient" % "4.5") ::
    ("org.scala-lang.modules" %% "scala-xml" % "1.0.2") ::
    ("com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3") ::
    ("com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3") ::
    ("com.typesafe" % "config" % "1.2.1") ::
    ("nu.validator.htmlparser" % "htmlparser" % "1.4") ::
    ("com.typesafe.scala-logging" %% "scala-logging" % "3.1.0") ::
    ("org.slf4j" % "slf4j-api" % "1.7.12") ::
    ("ch.qos.logback" % "logback-classic" % "1.1.3") ::
    ("org.scala-sbt" %% "io" % sbtVersion.value) ::
    ("com.github.tototoshi" %% "scala-csv" % "1.2.2") ::
    ("net.lingala.zip4j" % "zip4j" % "1.3.2") ::
    ("org.scalatest" % "scalatest_2.11" % "2.2.1" % "test") ::
    Nil
  )

assemblySettings

mainClass in assembly := Some("ganesa.MainGanesa")

jarName in assembly := { s"${name.value}.jar" }

