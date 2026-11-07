package com.pulumi.example.minimalmill

import com.pulumi.Pulumi
import scala.util.control.NonFatal

object App {
  def main(args: Array[String]): Unit = {
    // Mill reports only "Subprocess failed" when its forked JVM fails. Record
    // the SDK result so CI can distinguish an SDK failure from a launcher failure.
    val exitCode = try {
      Pulumi.runAsync { ctx =>
        val log = ctx.log()
        val config = ctx.config()
        val name = config.require("name")
        val secret = config.require("secret")
        log.info("Hello, %s!%n", name)
        log.info("Psst, %s%n", secret)
      }.join().intValue()
    } catch {
      case NonFatal(error) =>
        error.printStackTrace(System.err)
        throw error
    }
    System.err.println(s"Pulumi.runAsync returned exit code $exitCode")
    System.exit(exitCode)
  }
}
