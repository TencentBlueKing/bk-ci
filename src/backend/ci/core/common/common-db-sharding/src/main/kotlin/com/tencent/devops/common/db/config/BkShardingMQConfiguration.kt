/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.db.config

import com.tencent.devops.common.db.listener.BkShardingRoutingRuleListener
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.text.MessageFormat

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureAfter(BkShardingRoutingRuleListener::class)
class BkShardingMQConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(BkShardingMQConfiguration::class.java)
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun shardingRoutingRuleQueue() = Queue(BkServiceUtil.getDynamicMqQueue())

    @Bean
    fun shardingRoutingRuleExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_SHARDING_ROUTING_RULE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun shardingRoutingRuleQueueBind(
        @Autowired shardingRoutingRuleQueue: Queue,
        @Autowired shardingRoutingRuleExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(shardingRoutingRuleQueue).to(shardingRoutingRuleExchange)

    @Bean
    fun shardingRoutingRuleListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired messageConverter: Jackson2JsonMessageConverter,
        @Autowired shardingRoutingRuleQueue: Queue,
        @Autowired bkShardingRoutingRuleListener: BkShardingRoutingRuleListener
    ): SimpleMessageListenerContainer {
        // 增加动态队列清理钩子
        addDynamicMqQueueClearHook(rabbitAdmin, shardingRoutingRuleQueue)
        val adapter = MessageListenerAdapter(bkShardingRoutingRuleListener, bkShardingRoutingRuleListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = shardingRoutingRuleQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 10,
            concurrency = 1,
            maxConcurrency = 5
        )
    }

    private fun addDynamicMqQueueClearHook(rabbitAdmin: RabbitAdmin, shardingRoutingRuleQueue: Queue) {
        try {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    val queueName = shardingRoutingRuleQueue.name
                    logger.info("delete dynamicMqQueue($queueName) start!")
                    rabbitAdmin.purgeQueue(queueName)
                    rabbitAdmin.deleteQueue(queueName)
                    val queueProperties = rabbitAdmin.getQueueProperties(queueName)
                    logger.info("delete dynamicMqQueue($queueName) queueProperties:$queueProperties")
                    if (queueProperties != null) {
                        // 队列属性不为空说明删除未成功，打印失败日志
                        logger.info("delete dynamicMqQueue($queueName) fail!")
                        return
                    }
                    logger.info("delete dynamicMqQueue($queueName) success!")
                }
            })
        } catch (t: Throwable) {
            logger.warn("Fail to add dynamicMqQueueClear shutdown hook", t)
        }
    }
}
