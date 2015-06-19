name := "purescript-truffle-eval"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javacOptions ++= Seq("-source", "1.7")

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.0.4",
  "org.specs2" %% "specs2-core" % "3.6.1" % "test",
  "com.oracle" % "truffle" % "0.7",
  "com.oracle" % "truffle-dsl-processor" % "0.7"
)

compileOrder := CompileOrder.JavaThenScala
