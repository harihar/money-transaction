package com.revolut.utils

import org.slf4j.LoggerFactory
import spark.Spark

fun startServer(routeInit: () -> Unit): Int {
    Thread.sleep(500)
    val logger = LoggerFactory.getLogger("com.revolut.utils.ServerHelpers")
    logger.debug("Starting test http server")
    Spark.port(0)
    routeInit()
    Spark.awaitInitialization()
    val port = Spark.port()
    logger.debug("Started test http server at port: $port")
    return port
}
