/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.config

import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.provider.rbac.listener.AuthItsmCallbackListener
import com.tencent.devops.auth.provider.rbac.listener.AuthProjectLevelPermissionsSyncListener
import com.tencent.devops.auth.provider.rbac.listener.AuthResourceGroupCreateListener
import com.tencent.devops.auth.provider.rbac.listener.AuthResourceGroupModifyListener
import com.tencent.devops.auth.provider.rbac.listener.SyncGroupAndMemberListener
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthItsmCallbackEvent
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthProjectLevelPermissionsSyncEvent
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthResourceGroupCreateEvent
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthResourceGroupModifyEvent
import com.tencent.devops.auth.provider.rbac.service.PermissionGradeManagerService
import com.tencent.devops.auth.provider.rbac.service.PermissionSubsetManagerService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@Suppress("TooManyFunctions")
class RbacMQConfiguration {

    @Bean
    fun traceEventDispatcher(streamBridge: StreamBridge) = TraceEventDispatcher(streamBridge)

    @Bean
    fun syncGroupAndMemberListener(
        resourceGroupSyncService: PermissionResourceGroupSyncService,
        resourceGroupPermissionService: PermissionResourceGroupPermissionService,
        permissionMigrateService: PermissionMigrateService
    ) = SyncGroupAndMemberListener(
        resourceGroupSyncService = resourceGroupSyncService,
        resourceGroupPermissionService = resourceGroupPermissionService,
        permissionMigrateService = permissionMigrateService
    )

    @EventConsumer
    fun syncGroupAndMemberConsumer(
        @Autowired syncGroupAndMemberListener: SyncGroupAndMemberListener
    ) = ScsConsumerBuilder.build<ProjectEnableStatusBroadCastEvent> {
        syncGroupAndMemberListener.execute(it)
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

    @EventConsumer
    fun authItsmCallbackConsumer(
        @Autowired authItsmCallbackListener: AuthItsmCallbackListener
    ) = ScsConsumerBuilder.build<AuthItsmCallbackEvent> { authItsmCallbackListener.execute(it) }

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
    fun authProjectLevelPermissionsSyncListener(
        permissionService: PermissionResourceGroupPermissionService,
        traceEventDispatcher: TraceEventDispatcher
    ) = AuthProjectLevelPermissionsSyncListener(
        permissionService = permissionService,
        traceEventDispatcher = traceEventDispatcher
    )

    @EventConsumer
    fun authResourceGroupCreateConsumer(
        @Autowired authResourceGroupCreateListener: AuthResourceGroupCreateListener
    ) = ScsConsumerBuilder.build<AuthResourceGroupCreateEvent> { authResourceGroupCreateListener.execute(it) }

    @EventConsumer
    fun authProjectLevelPermissionsSyncConsumer(
        @Autowired authProjectLevelPermissionsSyncListener: AuthProjectLevelPermissionsSyncListener
    ) = ScsConsumerBuilder.build<AuthProjectLevelPermissionsSyncEvent> {
        authProjectLevelPermissionsSyncListener.execute(it)
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

    @EventConsumer
    fun authResourceGroupModifyConsumer(
        @Autowired authResourceGroupModifyListener: AuthResourceGroupModifyListener
    ) = ScsConsumerBuilder.build<AuthResourceGroupModifyEvent> { authResourceGroupModifyListener.execute(it) }
}
