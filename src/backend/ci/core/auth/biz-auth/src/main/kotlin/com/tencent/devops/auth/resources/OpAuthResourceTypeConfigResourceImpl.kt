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

package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.op.OpAuthResourceTypeConfigResource
import com.tencent.devops.auth.pojo.request.ActionCreateRequest
import com.tencent.devops.auth.pojo.request.FullResourceTypeConfigCreateRequest
import com.tencent.devops.auth.pojo.request.ProjectGroupConfigUpdateRequest
import com.tencent.devops.auth.pojo.request.ResourceGroupConfigCreateRequest
import com.tencent.devops.auth.pojo.request.ResourceTypeCreateRequest
import com.tencent.devops.auth.pojo.vo.ActionVO
import com.tencent.devops.auth.pojo.vo.ResourceGroupConfigVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeVO
import com.tencent.devops.auth.service.AuthResourceTypeConfigService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAuthResourceTypeConfigResourceImpl @Autowired constructor(
    private val authResourceTypeConfigService: AuthResourceTypeConfigService
) : OpAuthResourceTypeConfigResource {

    // ==================== 资源类型管理 ====================

    override fun listResourceTypes(): Result<List<ResourceTypeVO>> {
        return Result(authResourceTypeConfigService.listResourceTypes())
    }

    override fun getResourceType(resourceType: String): Result<ResourceTypeVO?> {
        return Result(authResourceTypeConfigService.getResourceType(resourceType))
    }

    override fun createResourceType(request: ResourceTypeCreateRequest): Result<Int> {
        return Result(authResourceTypeConfigService.createResourceType(request))
    }

    override fun deleteResourceType(resourceType: String): Result<Boolean> {
        return Result(authResourceTypeConfigService.deleteResourceType(resourceType))
    }

    // ==================== 操作管理 ====================

    override fun listActions(resourceType: String?): Result<List<ActionVO>> {
        return Result(authResourceTypeConfigService.listActions(resourceType))
    }

    override fun getAction(action: String): Result<ActionVO?> {
        return Result(authResourceTypeConfigService.getAction(action))
    }

    override fun createAction(request: ActionCreateRequest): Result<Boolean> {
        return Result(authResourceTypeConfigService.createAction(request))
    }

    override fun batchCreateActions(requests: List<ActionCreateRequest>): Result<Int> {
        return Result(authResourceTypeConfigService.batchCreateActions(requests))
    }

    override fun deleteAction(action: String): Result<Boolean> {
        return Result(authResourceTypeConfigService.deleteAction(action))
    }

    // ==================== 用户组配置管理 ====================

    override fun listGroupConfigs(resourceType: String?): Result<List<ResourceGroupConfigVO>> {
        return Result(authResourceTypeConfigService.listGroupConfigs(resourceType))
    }

    override fun getGroupConfig(id: Long): Result<ResourceGroupConfigVO?> {
        return Result(authResourceTypeConfigService.getGroupConfig(id))
    }

    override fun createGroupConfig(request: ResourceGroupConfigCreateRequest): Result<Long> {
        return Result(authResourceTypeConfigService.createGroupConfig(request))
    }

    override fun batchCreateGroupConfigs(requests: List<ResourceGroupConfigCreateRequest>): Result<Int> {
        return Result(authResourceTypeConfigService.batchCreateGroupConfigs(requests))
    }

    override fun appendActionsToGroupConfig(
        id: Long,
        resourceType: String,
        actions: List<String>
    ): Result<Boolean> {
        return Result(authResourceTypeConfigService.appendActionsToGroupConfig(id, resourceType, actions))
    }

    override fun appendActionsToExistingScope(
        id: Long,
        targetResourceType: String,
        actions: List<String>
    ): Result<Boolean> {
        return Result(
            authResourceTypeConfigService.appendActionsToExistingScope(id, targetResourceType, actions)
        )
    }

    override fun smartAppendActions(
        id: Long,
        resourceType: String,
        actions: List<String>
    ): Result<Boolean> {
        return Result(authResourceTypeConfigService.smartAppendActions(id, resourceType, actions))
    }

    override fun batchAppendActionsToProjectGroups(
        requests: List<ProjectGroupConfigUpdateRequest>
    ): Result<Int> {
        return Result(authResourceTypeConfigService.batchAppendActionsToProjectGroups(requests))
    }

    override fun batchSmartAppendActions(
        requests: List<ProjectGroupConfigUpdateRequest>
    ): Result<Int> {
        return Result(authResourceTypeConfigService.batchSmartAppendActions(requests))
    }

    override fun deleteGroupConfig(id: Long): Result<Boolean> {
        return Result(authResourceTypeConfigService.deleteGroupConfig(id))
    }

    // ==================== 便捷接口：一键创建完整资源类型配置 ====================

    override fun createFullResourceTypeConfig(
        request: FullResourceTypeConfigCreateRequest
    ): Result<Boolean> {
        return Result(
            authResourceTypeConfigService.createFullResourceTypeConfig(
                request.resourceType,
                request.actions,
                request.groupConfigs
            )
        )
    }
}
