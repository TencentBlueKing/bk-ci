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

package com.tencent.devops.auth.provider.rbac.listener

import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class SyncGroupAndMemberListener @Autowired constructor(
    private val resourceGroupSyncService: PermissionResourceGroupSyncService,
    private val resourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionMigrateService: PermissionMigrateService
) : EventListener<ProjectEnableStatusBroadCastEvent> {

    override fun execute(event: ProjectEnableStatusBroadCastEvent) {
        logger.info("sync permissions when enabled project $event")
        with(event) {
            if (enabled) {
                // 项目启用时，重新迁移/同步用户组/用户组成员/用户组权限
                permissionMigrateService.resetPermissionsWhenEnabledProject(projectId)
                resourceGroupSyncService.syncProjectGroup(projectId)
                resourceGroupSyncService.syncGroupAndMember(projectId)
                resourceGroupPermissionService.syncProjectPermissions(projectId)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyncGroupAndMemberListener::class.java)
    }
}
