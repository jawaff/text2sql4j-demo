package com.text2sql4j.api

import com.text2sql4j.api.stores.MovieStore
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TestSession : TestExecutionListener {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(TestSession::class.java)

        @JvmStatic
        private var isStarted: Boolean = false

        @JvmStatic
        lateinit var vertx: Vertx

        @JvmStatic
        lateinit var movieStore: MovieStore

        fun clearDatabase() {
            runBlocking {
                TestSession.movieStore.deleteCasts()
                TestSession.movieStore.deleteMovies()
                TestSession.movieStore.deleteActors()
            }
        }
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (!isStarted) {
            runBlocking {
                vertx = startVertx(doInsertDataset = false)

                val databaseHost = "localhost"
                val databasePort = 5432
                val databaseName = "movies"
                val databaseUsername = "postgres"
                val databasePassword = "postgres"
                val connectionPoolSize = 20

                val pgPool = PgPool.pool(
                    vertx,
                    PgConnectOptions()
                        .setHost(databaseHost)
                        .setPort(databasePort)
                        .setDatabase(databaseName)
                        .setUser(databaseUsername)
                        .setPassword(databasePassword),
                    PoolOptions().setMaxSize(connectionPoolSize).setShared(true)
                )
                movieStore = MovieStore(pgPool)
            }

            isStarted = true
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        if (isStarted) {
            runBlocking {
                stopVertx(vertx)
            }
            isStarted = false
        }
    }
}