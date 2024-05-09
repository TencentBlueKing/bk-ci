package com.tencent.devops.remotedev.utils

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * remotedev 进行异步操作时不需要使用消息队列则使用固定线程池
 */
object AsyncUtil {
    val pool = ThreadPoolExecutor(
        10,
        100,
        10L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )
}