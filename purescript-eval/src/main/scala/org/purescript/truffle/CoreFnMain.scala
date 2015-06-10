package org.purescript.truffle

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{ Files, Paths }

import argonaut.Parse

object CoreFnMain {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      System.err.println("Usage: TODO")
      return
    }

    val path = Paths.get(args(0))
    val content = new String(Files.readAllBytes(path), UTF_8)

    val result = Parse.decodeEither[CoreFnModule](content)
    val frame = result.map(CoreFnEval.evaluate)
    frame.fold(error, CoreFnEval.dumpFrame)
  }
}
