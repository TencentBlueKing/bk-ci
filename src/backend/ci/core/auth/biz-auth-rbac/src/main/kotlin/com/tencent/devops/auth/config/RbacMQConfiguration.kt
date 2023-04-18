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
 *
 */

package com.tencent.devops.auth.config

import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.listener.AuthItsmCallbackListener
import com.tencent.devops.auth.listener.AuthResourceGroupCreateListener
import com.tencent.devops.auth.listener.AuthResourceGroupModifyListener
import com.tencent.devops.auth.service.PermissionGradeManagerService
import com.tencent.devops.auth.service.PermissionSubsetManagerService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@Suppress("TooManyFunctions")
class RbacMQConfiguration {

    @Bean
    fun traceEventDispatcher(rabbitTemplate: RabbitTemplate) = TraceEventDispatcher(rabbitTemplate)

    @Bean
    fun authRbacExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_AUTH_RBAC_LISTENER_EXCHANGE, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun itsmCallbackQueue(): Queue {
        return Queue(MQ.QUEUE_AUTH_ITSM_CALLBACK, true)
    }

    @Bean
    fun itsmCallbackBind(
        @Autowired itsmCallbackQueue: Queue,
        @Autowired authRbacExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(itsmCallbackQueue).to(authRbacExchange).with(MQ.ROUTE_AUTH_ITSM_CALLBACK)
    }

    @Bean
    fun authItsmCallbackListener(
        client: Client,
        dslContext: DSLContext,
        authItsmCallbackDao: AuthItsmCallbackDao,
        permissionGradeManagerService: PermissionGradeManagerService,
        traceEventDispatcher: TraceEventDispatcher
    ) = AuthItsmCallbackListener(
        client = client,
        dslContext = dslContext,
        authItsmCallbackDao = authItsmCallbackDao,
        permissionGradeManagerService = permissionGradeManagerService,
        traceEventDispatcher = traceEventDispatcher
    )

    @Bean
    fun itsmCallbackEventListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired itsmCallbackQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired itsmCallbackListener: AuthItsmCallbackListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(itsmCallbackListener, itsmCallbackListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = itsmCallbackQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun authResourceGroupCreateQueue(): Queue {
        return Queue(MQ.QUEUE_AUTH_RESOURCE_GROUP_CREATE, true)
    }

    @Bean
    fun authResourceGroupCreateBind(
        @Autowired authResourceGroupCreateQueue: Queue,
        @Autowired authRbacExchange: DirectExchange
    ): Binding {
        return BindingBuilder
            .bind(authResourceGroupCreateQueue)
            .to(authRbacExchange)
            .with(MQ.ROUTE_AUTH_RESOURCE_GROUP_CREATE)
    }

    @Bean
    fun authResourceGroupCreateListener(
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        traceEventDispatcher: TraceEventDispatcher
    ) = AuthResourceGroupCreateListener(
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        traceEventDispatcher = traceEventDispatcher
    )

    @Bean
    fun authResourceGroupCreateEventListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired authResourceGroupCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired authResourceGroupCreateListener: AuthResourceGroupCreateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            authResourceGroupCreateListener,
            authResourceGroupCreateListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = authResourceGroupCreateQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun authResourceGroupModifyQueue(): Queue {
        return Queue(MQ.QUEUE_AUTH_RESOURCE_GROUP_MODIFY, true)
    }

    @Bean
    fun authResourceGroupModifyBind(
        @Autowired authResourceGroupModifyQueue: Queue,
        @Autowired authRbacExchange: DirectExchange
    ): Binding {
        return BindingBuilder
            .bind(authResourceGroupModifyQueue)
            .to(authRbacExchange)
            .with(MQ.ROUTE_AUTH_RESOURCE_GROUP_MODIFY)
    }

    @Bean
    fun authResourceGroupModifyListener(
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        traceEventDispatcher: TraceEventDispatcher
    ) = AuthResourceGroupModifyListener(
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        traceEventDispatcher = traceEventDispatcher
    )

    @Bean
    fun authResourceGroupModifyEventListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired authResourceGroupModifyQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired authResourceGroupModifyListener: AuthResourceGroupModifyListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            authResourceGroupModifyListener,
            authResourceGroupModifyListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = authResourceGroupModifyQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 20
        )
    }
}
