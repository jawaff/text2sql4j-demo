package com.text2sql4j.translator

import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.nio.file.Paths

class TestSession : TestExecutionListener {
    companion object {
        @JvmStatic
        private var isStarted: Boolean = false

        @JvmStatic
        lateinit var translator: SqlTranslator
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (!isStarted) {
            translator = DjlSqlTranslator(Paths.get("../python-translator/"))
            isStarted = true
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        if (isStarted) {
            translator.close()
        }
    }
}