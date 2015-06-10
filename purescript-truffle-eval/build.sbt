name := "purescript-eval"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.0.4",
  "org.specs2" %% "specs2-core" % "3.6.1" % "test"
)

compileOrder := CompileOrder.JavaThenScala
