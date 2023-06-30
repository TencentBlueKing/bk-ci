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

import com.tencent.devops.common.api.constant.BK_CREATE
import com.tencent.devops.common.api.constant.BK_REVISE
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_SECOND_LEVEL_ADMIN_CREATE
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_SECOND_LEVEL_ADMIN_REVISE
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_USER_GROUP_CRATE_TIME
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_USER_RATING_ADMIN_CRATE_TIME
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_USER_REQUESTS_THE_PROJECT
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import java.time.LocalDateTime

object IamGroupUtils {

    private const val SYSTEM_DEFAULT_NAME = "BKCI"
    // 用户组默认6个月有效期
    const val DEFAULT_EXPIRED_AT = 180L

    fun buildIamGroup(projectName: String, groupName: String): String {
        return "$projectName-$groupName"
    }

    fun buildDefaultDescription(
        projectName: String,
        groupName: String,
        userId: String,
        language: String
    ): String {
        return MessageUtil.getMessageByLocale(
            messageCode = BK_USER_GROUP_CRATE_TIME,
            language = language,
            arrayOf(projectName, groupName, userId)
        ) + DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun buildGradeManagerName(projectName: String) = "$SYSTEM_DEFAULT_NAME-$projectName"

    fun buildManagerDescription(
        projectName: String,
        userId: String,
        language: String
    ): String {
        return MessageUtil.getMessageByLocale(
            messageCode = BK_USER_RATING_ADMIN_CRATE_TIME,
            language = language,
            arrayOf(projectName, userId)
        ) + DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    fun renameSystemLable(groupName: String): String {
        return groupName.substringAfterLast("-")
    }

    fun buildItsmDefaultReason(
        projectName: String,
        userId: String,
        isCreate: Boolean,
        language: String
    ): String {
        val createOrUpdate = MessageUtil.getMessageByLocale(
            messageCode = if (isCreate) BK_CREATE else BK_REVISE,
            language = language
        )
        return MessageUtil.getMessageByLocale(
            messageCode = BK_USER_REQUESTS_THE_PROJECT,
            language = language,
            params = arrayOf(userId, createOrUpdate, projectName)
        )
    }

    fun defaultRoleCheck(groupName: String): Boolean {
        val name = renameSystemLable(groupName)
        if (BkAuthGroup.contains(name)) {
            return true
        }
        return false
    }

    fun buildGroupStrategyName(resourceType: String, groupCode: String) = "${resourceType}_$groupCode"

    /**
     * 构建二级管理员用户组名称
     */
    fun buildSubsetManagerGroupName(resourceType: String, resourceName: String) =
        "$SYSTEM_DEFAULT_NAME-${resourceType.uppercase()}-$resourceName"

    /**
     * 构建二级管理员用户组名称
     */
    fun buildSubsetManagerGroupName(resourceType: String, resourceCode: String, resourceName: String) =
        "$SYSTEM_DEFAULT_NAME-${resourceType.uppercase()}-$resourceName-$resourceCode"

    /**
     * 获取二级管理员用户组展示名称
     */
    fun getGroupDisplayName(groupName: String) =
        groupName.substringAfterLast("-")

    /**
     * 构建二级管理员描述
     */
    fun buildSubsetManagerDescription(resourceName: String, userId: String, language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = BK_SECOND_LEVEL_ADMIN_CREATE,
            language = language,
            params = arrayOf(resourceName, userId)
        ) + DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    /**
     * 构建二级管理员描述
     */
    fun buildSubsetManagerUpdateDescription(resourceName: String, userId: String, language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = BK_SECOND_LEVEL_ADMIN_REVISE,
            language = language,
            params = arrayOf(resourceName, userId)
        ) + DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }

    /**
     * 构建二级管理员用户组描述
     */
    fun buildSubsetManagerGroupDescription(
        resourceName: String,
        groupName: String,
        userId: String,
        language: String
    ): String {
        return MessageUtil.getMessageByLocale(
            messageCode = BK_SECOND_LEVEL_ADMIN_CREATE,
            language = language,
            params = arrayOf(resourceName, groupName, userId)
        ) + DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
    }
}
