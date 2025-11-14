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

package com.tencent.devops.remotedev.resources.external

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.external.ExternalResource
import com.tencent.devops.remotedev.pojo.WhiteList
import com.tencent.devops.remotedev.pojo.WhiteListType
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WhiteListService.Companion.CONFIG_CDS_DOMAIN_WORKSPACE_KEY_PREFIX
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.software.SoftwareManageService
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ExternalResourceImpl @Autowired constructor(
    private val softwareManageService: SoftwareManageService,
    private val configCacheService: ConfigCacheService,
    private val whiteListService: WhiteListService,
    private val workspaceService: WorkspaceService
) : ExternalResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalResourceImpl::class.java)
    }

    /*请求合法性校验时使用的密钥*/
    @Value("\${externalKey:}")
    val externalKey = ""

    override fun softwareInstallCallback(
        type: String,
        key: String,
        projectId: String,
        userId: String,
        workspaceName: String,
        softwareList: SoftwareCallbackRes
    ): Result<Boolean> {
        if (key != externalKey) return Result(false)
        softwareManageService.softwareInstallationCompleteCallback(
            type = type,
            workspaceName = workspaceName,
            projectId = projectId,
            userId = userId,
            softwareList = softwareList
        )
        return Result(true)
    }

    override fun cdsMeshEnableAndDomain(
        ts: String,
        token: String,
        ip: String,
        enable: String,
        domain: String,
        sslMode: String?
    ): Result<Boolean> {
        logger.info("cdsMeshEnableAndDomain|enable=$enable|domain=$domain|ip=$ip|sslMode=$sslMode|ts=$ts|token=$token")
        
        // 验证请求
        if (!validateRequest(ts, token, ip)) {
            return Result(false)
        }
        
        // 查询工作空间
        val workspace = findWorkspaceByIp(ip) ?: run {
            logger.warn("no workspace found|ip=$ip")
            return Result(false)
        }
        
        // 处理网络配置 - 使用 Pair 模式匹配
        val isEnabled = enable.toBooleanStrictOrNull() == true
        val useSslMode = !sslMode.isNullOrBlank()
        
        when (isEnabled to useSslMode) {
            true to true -> {
                // 情况1: 启用 SSL 模式
                deleteWhiteList(workspace.workspaceName, WhiteListType.CDS_MESH_WORKSPACE)
                createWhiteList(workspace.workspaceName, WhiteListType.CDS_SSL_WORKSPACE)
            }
            true to false -> {
                // 情况2: 启用 Mesh 模式
                createWhiteList(workspace.workspaceName, WhiteListType.CDS_MESH_WORKSPACE)
            }
            false to true -> {
                // 情况3: 禁用 SSL 模式
                deleteWhiteList(workspace.workspaceName, WhiteListType.CDS_SSL_WORKSPACE)
            }
            false to false -> {
                // 情况4: 禁用 Mesh 模式
                deleteWhiteList(workspace.workspaceName, WhiteListType.CDS_MESH_WORKSPACE)
            }
        }
        
        // 处理域名配置 - 使用 Kotlin 惯用法
        domain.takeIf { it.isNotBlank() }?.let {
            configCacheService.opInsertOrUpdateConfig(
                CONFIG_CDS_DOMAIN_WORKSPACE_KEY_PREFIX + workspace.workspaceName,
                it
            )
        }
        
        return Result(true)
    }

    /**
     * 验证请求的时间戳和签名
     */
    private fun validateRequest(ts: String, token: String, ip: String): Boolean {
        // ts 10位时间戳需与当前时间相差小于10秒
        if (LocalDateTime.now().timestamp() - ts.toLong() > 10) {
            logger.warn("ts not match|ts=$ts")
            return false
        }
        val sign = ShaUtils.sha256("$ts$externalKey$ip")
        if (sign != token) {
            logger.warn("sign not match|sign=$sign|token=$token|ts=$ts|ip=$ip")
            return false
        }
        return true
    }

    /**
     * 根据 IP 查找工作空间
     */
    private fun findWorkspaceByIp(ip: String) = workspaceService.limitFetchProjectWorkspace(
        limit = SQLLimit(0, 1),
        queryType = QueryType.OP,
        search = WorkspaceSearch(sips = listOf(ip), onFuzzyMatch = false)
    ).firstOrNull()

    /**
     * 创建或更新白名单
     */
    private fun createWhiteList(workspaceName: String, type: WhiteListType) {
        whiteListService.opCreateOrUpdateWhiteList(
            SYSTEM,
            WhiteList(workspaceName, type, SYSTEM)
        )
    }

    /**
     * 删除白名单
     */
    private fun deleteWhiteList(workspaceName: String, type: WhiteListType) {
        whiteListService.opDeleteWhiteList(
            SYSTEM,
            WhiteList(workspaceName, type, SYSTEM)
        )
    }
}
