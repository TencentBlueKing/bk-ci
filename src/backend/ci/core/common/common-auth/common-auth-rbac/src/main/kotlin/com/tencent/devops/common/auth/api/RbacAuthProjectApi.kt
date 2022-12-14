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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.code.AuthServiceCode
import org.slf4j.LoggerFactory

class RbacAuthProjectApi : AuthProjectApi {

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return true
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return true
    }

    override fun checkProjectManager(userId: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        return true
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        return emptyList()
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        return emptyMap()
    }

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        return true
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        return emptyList()
    }

    override fun getProjectInfo(serviceCode: AuthServiceCode, projectId: String): BkAuthProjectInfoResources? {
        return null
    }

    private fun checkAction(projectCode: String, actionType: String, userId: String): Boolean {
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(RbacAuthProjectApi::class.java)
    }
}
