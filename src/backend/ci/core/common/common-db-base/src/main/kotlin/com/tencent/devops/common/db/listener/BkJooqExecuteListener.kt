package com.tencent.devops.common.db.listener

import org.jooq.ExecuteContext
import org.jooq.impl.DefaultExecuteListener
import org.jooq.tools.StopWatch
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class BkJooqExecuteListener : DefaultExecuteListener() {

    companion object {
        val thresholdNanos = TimeUnit.SECONDS.toNanos(1)
        private val logger = LoggerFactory.getLogger(BkJooqExecuteListener::class.java)
    }

    override fun executeStart(ctx: ExecuteContext) {
        super.executeStart(ctx)
        ctx.data(getStopWatchName(), StopWatch())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        super.executeEnd(ctx)
        val stopWatch = ctx.data(getStopWatchName()) as StopWatch
        val costTime = stopWatch.split()
        if (costTime > thresholdNanos) {
            // sql执行超时打印告警日志
            val sql = ctx.query().toString().replace(Regex("[\r\n]"), " ")
            logger.warn("Bk Slow SQL cost ${TimeUnit.NANOSECONDS.toMillis(costTime)} ms. SQL:[$sql]")
        }
    }

    private fun getStopWatchName() = "${this.javaClass.name}.watch"
}
