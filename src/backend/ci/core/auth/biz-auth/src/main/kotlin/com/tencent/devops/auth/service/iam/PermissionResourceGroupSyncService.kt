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

package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

/**
 * iam资源组同步服务
 */
interface PermissionResourceGroupSyncService {
    /**
     * 通过条件搜素项目，同步项目下组和成员
     */
    fun syncByCondition(projectConditionDTO: ProjectConditionDTO)

    /**
     * 通过条件搜素项目，同步用户组过期时间
     */
    fun syncGroupMemberExpiredTime(projectConditionDTO: ProjectConditionDTO)

    /**
     * 批量同步项目下组和成员
     */
    fun batchSyncGroupAndMember(projectCodes: List<String>)

    /**
     * 同步项目下组和成员
     */
    fun syncGroupAndMember(projectCode: String)

    /**
     * 同步项目下组
     */
    fun syncProjectGroup(projectCode: String)

    /**
     * 获取项目的同步状态
     */
    fun getStatusOfSync(projectCode: String): AuthMigrateStatus

    /**
     * 同步项目下用户组
     */
    fun batchSyncProjectGroup(projectCodes: List<String>)

    /**
     * 同步所有用户组成员
     */
    fun batchSyncAllMember(projectCodes: List<String>)

    /**
     * 同步资源成员
     */
    fun syncResourceMember(projectCode: String, resourceType: String, resourceCode: String)

    /**
     * 同步iam组成员
     */
    fun syncIamGroupMember(projectCode: String, iamGroupId: Int)

    /**
     * 同步iam组成员--用户申请加入
     */
    fun syncIamGroupMembersOfApply()

    /**
     * 防止出现用户组表的数据已经删了，但是用户组成员表的数据未删除，导致出现不同步，调用iam接口报错问题。
     */
    fun fixResourceGroupMember(projectCode: String)
}
