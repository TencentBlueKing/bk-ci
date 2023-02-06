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

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.HttpClientService
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.bk.sdk.iam.service.v2.impl.V2GrantServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2ManagerServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2PolicyServiceImpl
import com.tencent.devops.auth.dispatcher.AuthItsmCallbackDispatcher
import com.tencent.devops.auth.listener.AuthItsmCallbackListener
import com.tencent.devops.auth.listener.AuthResourceGroupListener
import com.tencent.devops.auth.service.AuthResourceGroupService
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionGradeManagerService
import com.tencent.devops.auth.service.PermissionSubsetManagerService
import com.tencent.devops.auth.service.RbacPermissionExtService
import com.tencent.devops.auth.service.RbacPermissionItsmCallbackService
import com.tencent.devops.auth.service.RbacPermissionProjectService
import com.tencent.devops.auth.service.RbacPermissionResourceGroupService
import com.tencent.devops.auth.service.RbacPermissionResourceService
import com.tencent.devops.auth.service.RbacPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@Suppress("TooManyFunctions")
class RbacAuthConfiguration {

    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.iamSystem:}")
    val systemId = ""

    @Value("\${auth.appCode:}")
    val appCode = ""

    @Value("\${auth.appSecret:}")
    val appSecret = ""

    @Value("\${auth.apigwUrl:#{null}}")
    val iamApigw = ""

    @Bean
    @ConditionalOnMissingBean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, iamApigw)

    @Bean
    fun apigwHttpClientServiceImpl() = ApigwHttpClientServiceImpl(iamConfiguration())

    @Bean
    fun iamV2ManagerService() = V2ManagerServiceImpl(apigwHttpClientServiceImpl(), iamConfiguration())

    @Bean
    fun iamV2PolicyService() = V2PolicyServiceImpl(apigwHttpClientServiceImpl(), iamConfiguration())

    @Bean
    fun grantV2Service() = V2GrantServiceImpl(apigwHttpClientServiceImpl(), iamConfiguration())

    @Bean
    fun tokenService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: HttpClientService
    ) = TokenServiceImpl(iamConfiguration, apigwHttpClientServiceImpl)

    @Bean
    fun authHelper(
        tokenService: TokenService,
        iamV2PolicyService: PolicyService,
        iamConfiguration: IamConfiguration
    ) = AuthHelper(tokenService, iamV2PolicyService, iamConfiguration)

    @Bean
    @SuppressWarnings("LongParameterList")
    fun permissionResourceService(
        client: Client,
        iamV2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        authResourceGroupService: AuthResourceGroupService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService
    ) = RbacPermissionResourceService(
        client = client,
        iamV2ManagerService = iamV2ManagerService,
        authResourceService = authResourceService,
        authResourceGroupService = authResourceGroupService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService
    )

    @Bean
    @SuppressWarnings("LongParameterList")
    fun permissionResourceGroupService(
        iamV2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        permissionResourceService: PermissionResourceService
    ) = RbacPermissionResourceGroupService(
        iamV2ManagerService = iamV2ManagerService,
        authResourceService = authResourceService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        permissionResourceService = permissionResourceService
    )

    @Bean
    @Primary
    fun rbacPermissionExtService(
        permissionResourceService: PermissionResourceService
    ) = RbacPermissionExtService(
        permissionResourceService = permissionResourceService,
    )

    @Bean
    @Primary
    fun permissionItsmCallbackService(
        authItsmCallbackDispatcher: AuthItsmCallbackDispatcher
    ) = RbacPermissionItsmCallbackService(
        authItsmCallbackDispatcher
    )

    @Bean
    @Primary
    fun rbacPermissionService(
        authHelper: AuthHelper,
        authResourceService: AuthResourceService,
        iamConfiguration: IamConfiguration
    ) = RbacPermissionService(authHelper, authResourceService, iamConfiguration)

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
    fun authResourceGroupQueue(): Queue {
        return Queue(MQ.QUEUE_AUTH_RESOURCE_GROUP, true)
    }

    @Bean
    fun authResourceGroupBind(
        @Autowired authResourceGroupQueue: Queue,
        @Autowired authRbacExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(authResourceGroupQueue).to(authRbacExchange).with(MQ.ROUTE_AUTH_RESOURCE_GROUP)
    }

    @Bean
    fun authResourceGroupEventListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired authResourceGroupQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired authResourceGroupListener: AuthResourceGroupListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(authResourceGroupListener, authResourceGroupListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = authResourceGroupQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun defaultPermissionProjectServiceImpl(
        client: Client,
        iamManagerService: V2ManagerService,
        deptService: DeptService,
        policyService: PolicyService
    ) = RbacPermissionProjectService(client, iamManagerService, deptService, policyService)
}
