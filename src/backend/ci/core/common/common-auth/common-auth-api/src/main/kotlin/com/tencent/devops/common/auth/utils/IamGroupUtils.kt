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

package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import java.time.LocalDateTime

object IamGroupUtils {

    private const val SYSTEM_DEFAULT_NAME = "蓝盾"
    // 用户组默认6个月有效期
    const val DEFAULT_EXPIRED_AT = 180L

    fun buildIamGroup(projectName: String, groupName: String): String {
        return "$projectName-$groupName"
    }

    fun buildDefaultDescription(projectName: String, groupName: String, userId: String): String {
        return "$projectName 用户组:$groupName,由$userId 创建于 " +
            DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun buildManagerDescription(projectName: String, userId: String): String {
        return "$projectName 分级管理员, 由$userId 创建于" +
            DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun buildManagerUpdateDescription(projectName: String, userId: String): String {
        return "$projectName 分级管理员, 由$userId 修改于" +
            DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun renameSystemLable(groupName: String): String {
        return groupName.substringAfterLast("-")
    }

    fun buildItsmDefaultReason(projectName: String, userId: String, isCreate: Boolean): String {
        val createOrUpdate = if (isCreate) "创建" else "修改"
        return "用户 $userId 申请${createOrUpdate}蓝盾项目 $projectName ,请审批！"
    }

    fun defaultRoleCheck(groupName: String): Boolean {
        val name = renameSystemLable(groupName)
        if (BkAuthGroup.contains(name)) {
            return true
        }
        return false
    }

    fun buildSubsetManagerGroupStrategyName(resourceType: String, groupCode: String) = "${resourceType}_$groupCode"

    fun buildSubsetManagerGroupName(resourceName: String, groupName: String) =
        "${SYSTEM_DEFAULT_NAME}_${resourceName}_${groupName}"

    fun buildSubsetManagerDescription(resourceName: String, userId: String): String {
        return "$resourceName 二级管理员, 由$userId 创建于" +
            DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun buildSubsetManagerGroupDescription(resourceName: String, groupName: String, userId: String): String {
        return "$resourceName 用户组:$groupName, 由$userId 创建于" +
            DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")

    }
}
