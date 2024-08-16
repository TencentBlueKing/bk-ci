package com.tencent.devops.dispatch.macos.constant

object Constant {
    /**
     * 构建任务消费最大并发数
     */
    const val MAX_STARTUP_CONCURRENCY = 30

    /**
     * 降级队列构建任务消费最大并发数
     */
    const val MAX_DEMOTE_STARTUP_CONCURRENCY = 2
}
