package com.tencent.devops.common.db.listener

import org.jooq.ExecuteContext
import org.jooq.impl.DefaultExecuteListener
import org.jooq.tools.StopWatch
import org.slf4j.LoggerFactory

class BkJooqExecuteListener : DefaultExecuteListener() {

    companion object {
        private val logger = LoggerFactory.getLogger(BkJooqExecuteListener::class.java)
    }

    override fun executeStart(ctx: ExecuteContext) {
        super.executeStart(ctx)
        ctx.data(getStopWatchName(), StopWatch())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        super.executeEnd(ctx)
        val stopWatch = ctx.data(getStopWatchName()) as StopWatch
        val costTime = stopWatch.split() / 1000000 // 单位：毫秒
        if (costTime > 1000) {
            // sql执行超时打印告警日志
            logger.warn("Bk Slow SQL:[${ctx.query()}] cost $costTime ms")
        }
    }

    private fun getStopWatchName() = "${this.javaClass.name}.watch"
}
