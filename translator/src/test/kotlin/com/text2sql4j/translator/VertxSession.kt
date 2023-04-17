package com.text2sql4j.translator

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.nio.file.Paths

class VertxSession : TestExecutionListener {
    companion object {
        @JvmStatic
        var vertx: Vertx? = null
        @JvmStatic
        lateinit var translator: SqlTranslator
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (vertx == null) {
            vertx = Vertx.vertx()

            DatabindCodec.mapper()
                .registerKotlinModule()

            translator = DjlSqlTranslator(Paths.get("../python-translator/"))
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        if (vertx != null) {
            runBlocking {
                vertx!!.close().await()
            }
        }
    }
}