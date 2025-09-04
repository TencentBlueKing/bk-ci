package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.remotedev.listener.AsyncExecuteListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * remotedev 服务内作为更安全的线程池的异步实现方式
 */
@Configuration
class AsyncExecuteMq {
    @EventConsumer
    fun asyncExecuteConsumer(
        @Autowired asyncExecuteListener: AsyncExecuteListener
    ) = ScsConsumerBuilder.build<AsyncExecuteEvent> { asyncExecuteListener.listenAsyncExecuteEvent(it) }
}
