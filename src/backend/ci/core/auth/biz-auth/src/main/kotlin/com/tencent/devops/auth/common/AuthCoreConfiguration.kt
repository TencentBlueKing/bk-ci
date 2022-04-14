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

package com.tencent.devops.auth.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.dao.ActionDao
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.filter.TokenCheckFilter
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.listener.AuthRefreshEventListener
import com.tencent.devops.auth.service.DefaultDeptServiceImpl
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.EmptyPermissionExtServiceImpl
import com.tencent.devops.auth.service.EmptyPermissionUrlServiceImpl
import com.tencent.devops.auth.service.action.ActionService
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.auth.service.action.impl.SimpleBkActionServiceImpl
import com.tencent.devops.auth.service.action.impl.SimpleBkResourceServiceImpl
import com.tencent.devops.auth.utils.HostUtils
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import org.jooq.DSLContext
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Suppress("ALL")
@Configuration
class AuthCoreConfiguration {

    @Value("\${devopsGateway.idc:#{null}}")
    private val devopsGateway: String? = null

    @Bean
    fun refreshDispatch(rabbitTemplate: RabbitTemplate) = AuthRefreshDispatch(rabbitTemplate)

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun authCoreExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_AUTH_REFRESH_FANOUT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }
//
//    @Bean
//    fun authCoreExchange(): FanoutExchange {
//        val directExchange = FanoutExchange(MQ.EXCHANGE_AUTH_REFRESH_FANOUT, true, false)
//        directExchange.isDelayed = true
//        return directExchange
//    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun authRefreshQueue(): Queue {
        val hostIp = HostUtils.getHostIp(devopsGateway)
        return Queue(MQ.QUEUE_AUTH_REFRESH_EVENT + "." + hostIp, true, false, true)
    }

    @Bean
    fun refreshQueueBind(
        @Autowired authRefreshQueue: Queue,
        @Autowired authCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(authRefreshQueue).to(authCoreExchange).with(MQ.ROUTE_AUTH_REFRESH_FANOUT)
    }

//    @Bean
//    fun refreshQueueBind(
//        @Autowired authRefreshQueue: Queue,
//        @Autowired authCoreExchange: FanoutExchange
//    ): Binding {
//        return BindingBuilder.bind(authRefreshQueue).to(authCoreExchange)
//    }

    @Bean
    fun authRefreshEventListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired authRefreshQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired refreshListener: AuthRefreshEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(authRefreshQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(5000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(refreshListener, refreshListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    @ConditionalOnMissingBean(DeptService::class)
    fun defaultDeptServiceImpl() = DefaultDeptServiceImpl()

    @Bean
    fun tokenFilter(clientTokenService: ClientTokenService) = TokenCheckFilter(clientTokenService)

    @Bean
    @ConditionalOnMissingBean(name = ["permissionExtService"])
    fun permissionExtService() = EmptyPermissionExtServiceImpl()

    @Bean
    @ConditionalOnMissingBean(name = ["permissionUrlService"])
    fun permissionUrlService() = EmptyPermissionUrlServiceImpl()

    @Bean
    @ConditionalOnMissingBean(ActionService::class)
    fun simpleActionService(
        dslContext: DSLContext,
        actionDao: ActionDao,
        resourceService: BkResourceService
    ) = SimpleBkActionServiceImpl(dslContext, actionDao, resourceService)

    @Bean
    @ConditionalOnMissingBean(BkResourceService::class)
    fun simpleResourceService(
        dslContext: DSLContext,
        resourceDao: ResourceDao
    ) = SimpleBkResourceServiceImpl(dslContext, resourceDao)
}
