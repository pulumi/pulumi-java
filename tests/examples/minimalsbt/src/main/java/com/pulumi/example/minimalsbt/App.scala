package com.pulumi.example.minimalsbt

import com.pulumi.Pulumi

object App {
  def main(args: Array[String]): Unit = Pulumi.run { ctx =>
    val log = ctx.log()
    val config = ctx.config()
    val name = config.require("name")
    val secret = config.require("secret")
    log.info("Hello, %s!%n", name)
    log.info("Psst, %s%n", secret)
  }
}
