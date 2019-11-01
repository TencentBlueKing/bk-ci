package com.tencent.devops.process.bkjob

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.process.engine.init.Tools
import com.tencent.devops.process.util.CommonUtils
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建扩展配置
 */
@Configuration
class BKJobMQConfiguration {

    /**
     * 构建广播交换机
     */
    @Bean
    fun bkJobFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_BKJOB_CLEAR_JOB_TMP_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Value("\${queueConcurrency.bkjob:1}")
    private val bkJobConcurrency: Int? = null

    /**
     * 按IP绑定的清理文件队列--- 并发小
     */
    @Bean
    fun bkJobClearFileQueue() = Queue("${MQ.QUEUE_BKJOB_CLEAR_JOB_TMP_EVENT}.${CommonUtils.getInnerIP()}")

    @Bean
    fun bkJobClearFileQueueBind(
        @Autowired bkJobClearFileQueue: Queue,
        @Autowired bkJobFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(bkJobClearFileQueue).to(bkJobFanoutExchange)
    }

    @Bean
    fun bkJobClearFileListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired bkJobClearFileQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: BKJobClearTempFileListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = bkJobClearFileQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 600000,
            consecutiveActiveTrigger = 100,
            concurrency = bkJobConcurrency!!,
            maxConcurrency = 3
        )
    }
}