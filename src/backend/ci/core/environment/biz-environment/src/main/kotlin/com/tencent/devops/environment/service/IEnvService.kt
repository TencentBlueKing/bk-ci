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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.enums.EnvType

interface IEnvService {
    fun checkName(projectId: String, envId: Long?, envName: String)
    fun createEnvironment(userId: String, projectId: String, envCreateInfo: EnvCreateInfo): EnvironmentId
    fun updateEnvironment(userId: String, projectId: String, envHashId: String, envUpdateInfo: EnvUpdateInfo)
    fun listEnvironment(
        userId: String,
        projectId: String,
        envName: String? = null,
        envType: EnvType? = null,
        nodeHashId: String? = null
    ): List<EnvWithPermission>

    fun listUsableServerEnvs(userId: String, projectId: String): List<EnvWithPermission>
    fun listEnvironmentByType(userId: String, projectId: String, envType: EnvType): List<EnvWithNodeCount>
    fun listEnvironmentByLimit(projectId: String, offset: Int?, limit: Int?): Page<EnvWithPermission>
    fun listBuildEnvs(userId: String, projectId: String, os: OS): List<EnvWithNodeCount>
    fun getEnvironment(
        userId: String,
        projectId: String,
        envHashId: String,
        checkPermission: Boolean = true
    ): EnvWithPermission

    fun listRawEnvByHashIds(userId: String, projectId: String, envHashIds: List<String>): List<EnvWithPermission>
    fun listRawEnvByHashIdsAllType(envHashIds: List<String>): List<EnvWithPermission>
    fun listRawEnvByEnvNames(userId: String, projectId: String, envNames: List<String>): List<EnvWithPermission>
    fun deleteEnvironment(userId: String, projectId: String, envHashId: String)
    fun listRawServerNodeByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Map<String, List<NodeBaseInfo>>

    fun listAllEnvNodes(userId: String, projectId: String, envHashIds: List<String>): List<NodeBaseInfo>
    fun listAllEnvNodesNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        envHashIds: List<String>
    ): Page<NodeBaseInfo>

    fun addEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>)
    fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>)
    fun searchByName(projectId: String, envName: String, limit: Int, offset: Int): Page<EnvWithPermission>
}
