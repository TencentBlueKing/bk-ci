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
 */

package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.vo.AuthProjectVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus

interface PermissionAuthorizationService {
    /**
     * 增加资源授权管理
     */
    fun addResourceAuthorization(
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ): Boolean

    /**
     * 迁移资源授权管理
     */
    fun migrateResourceAuthorization(
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ): Boolean

    /**
     * 获取资源授权记录
     */
    fun getResourceAuthorization(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        executePermissionCheck: Boolean = false
    ): ResourceAuthorizationResponse

    /**
     * 当移出用户组时做授权检查
     */
    fun checkAuthorizationWhenRemoveGroupMember(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        memberId: String
    ): Boolean

    /**
     * 获取项目资源授予记录--根据条件
     */
    fun listResourceAuthorizations(
        condition: ResourceAuthorizationConditionRequest,
        operateChannel: OperateChannel? = OperateChannel.MANAGER
    ): SQLPage<ResourceAuthorizationResponse>

    /**
     * 获取用户授权相关项目
     */
    fun listUserProjectsWithAuthorization(
        userId: String
    ): List<AuthProjectVO>

    /**
     * 修改资源授权管理
     */
    fun modifyResourceAuthorization(
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ): Boolean

    /**
     * 是否用户拥有项目下授权
     */
    fun isUserHasProjectAuthorizations(
        projectCode: String,
        userId: String
    ): Boolean

    /**
     * 删除资源授权管理
     */
    fun deleteResourceAuthorization(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 修复迁移产生的脏数据
     */
    fun fixResourceAuthorization(
        projectCode: String,
        resourceType: String,
        resourceAuthorizationIds: List<String>
    ): Boolean

    /**
     * 批量重置授权人
     */
    fun batchModifyHandoverFrom(
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    ): Boolean

    /**
     * 批量重置授权人--根据资源类型
     */
    fun resetResourceAuthorizationByResourceType(
        operator: String,
        projectCode: String,
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>

    /**
     * 交接授权申请
     */
    fun handoverAuthorizationsApplication(
        operator: String,
        projectCode: String,
        condition: ResourceAuthorizationHandoverConditionRequest
    ): String

    /**
     * 批量重置授权人--项目下全量
     */
    fun resetAllResourceAuthorization(
        operator: String,
        projectCode: String,
        condition: ResetAllResourceAuthorizationReq
    ): List<ResourceTypeInfoVo>

    /**
     * 检查交接人是否有代码库授权权限
     */
    fun checkRepertoryAuthorizationsHanover(
        operator: String,
        projectCode: String,
        repertoryIds: List<String>,
        handoverFrom: String,
        handoverTo: String
    )
}
