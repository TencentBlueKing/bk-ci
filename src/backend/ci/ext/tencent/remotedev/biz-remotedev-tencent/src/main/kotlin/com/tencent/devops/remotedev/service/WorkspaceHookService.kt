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

package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.devx.ServiceDEVXResource
import com.tencent.devops.environment.pojo.DEVXHook
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import java.util.Base64
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class WorkspaceHookService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val bkConfig: BkConfig,
    private val client: Client,
    private val configCacheService: ConfigCacheService,
    private val workspaceJoinDao: WorkspaceJoinDao
) {

    private data class Actions(
        val action: List<Action>
    ) {
        data class Action(
            val type: String,
            val executables: List<String>
        )

        enum class Type {
            ENTER,
            EXIT;

            companion object {
                fun load(input: DEVXHook.ExecutionType) = when (input) {
                    DEVXHook.ExecutionType.LOG_IN -> ENTER
                    DEVXHook.ExecutionType.LOG_OUT -> EXIT
                }
            }
        }
    }

    private data class RequestBody(
        val name: String,
        val constants: Map<String, String>
    )

    private data class Response(
        val result: Boolean,
        val data: Data?
    ) {
        data class Data(
            @JsonProperty("task_id")
            val taskId: Int,
            @JsonProperty("task_url")
            val taskUrl: String
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceHookService::class.java)
    }

    private fun DEVXHook.executableCommand(): String {
        return configCacheService.get("DEVXHook:${hookType.name}:${executionType.name}") ?: ""
    }

    private fun getEnvAllIp(userId: String, projectId: String, envHashId: String): Set<String> {
        val nodeHashIds = client.get(ServiceEnvironmentResource::class).listNodesByEnvIds(
            userId, projectId, listOf(envHashId)
        ).data?.map { it.nodeHashId }?.toSet() ?: return setOf()

        val workspaces = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            nodeHashId = nodeHashIds,
            checkField = listOf(
                TWorkspace.T_WORKSPACE.PROJECT_ID,
                TWorkspace.T_WORKSPACE.STATUS,
                TWorkspace.T_WORKSPACE.IP
            )
        )
        val ips = mutableSetOf<String>()
        workspaces.forEach { workspace ->
            if (!workspace.status.checkRunning()) {
                logger.warn(
                    "WorkspaceHookService|ip: ${workspace.ip}|workapce name: ${workspace.workspaceName}" +
                        "|status: ${workspace.status}"
                )
            }
            if (workspace.projectId != projectId) {
                logger.error("WorkspaceHookService|wrong project id |${workspace.projectId}|$projectId")
                return@forEach
            }
            if (workspace.ip == null) {
                logger.error("WorkspaceHookService|null ip.")
                return@forEach
            }
            ips.add(checkNotNull(workspace.ip))
        }
        return ips
    }

    fun hookLoad(userId: String, projectId: String, envHashId: String, ip: List<String>?) {
        logger.info("hookLoad|$userId|$projectId|$envHashId|$ip")
        val load = client.get(ServiceDEVXResource::class)
            .getEnvHook(userId = userId, projectId = projectId, envHashId = envHashId).data!!
        installHook(ip?.ifEmpty { null }?.let { ip.toSet() } ?: getEnvAllIp(userId, projectId, envHashId), load)
    }

    fun hookDelete(userId: String, projectId: String, envHashId: String, ip: List<String>?) {
        logger.info("hookDelete|$userId|$projectId|$envHashId|$ip")
        uninstallHook(ip?.ifEmpty { null }?.let { ip.toSet() } ?: getEnvAllIp(userId, projectId, envHashId))
    }

    private fun installHook(ip: Set<String>, hooks: List<DEVXHook>) {
        if (ip.isEmpty() || hooks.isEmpty()) {
            logger.warn("installHook|ip: $ip, hooks: $hooks")
        }
        val actions = hooks.groupBy { it.executionType }.map { (executionType, group) ->
            val executables = group.map { it.executableCommand() }
            Actions.Action(
                type = Actions.Type.load(executionType).name.lowercase(),
                executables = executables
            )
        }
        val actionString = JsonUtil.toJson(Actions(actions), false)
        val req = RequestBody(
            name = "钩子安装",
            constants = mapOf(
                "\${vm_ip}" to ip.joinToString(","),
                "\${operation_type}" to "install",
                "\${base64EncodedString}" to Base64.getEncoder().encodeToString(actionString.toByteArray())
            )
        )
        val body = JsonUtil.toJson(req, false)
        logger.info("installHook|request url: ${bkConfig.bksopsCreateTask}, body: $body")
        val request = Request.Builder()
            .url(bkConfig.bksopsCreateTask)
            .headers(headers())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = OkhttpUtils.doHttp(request).resolveResponse<Response>()
        logger.info("installHook|task status: ${resp.result}|task url: ${resp.data?.taskUrl}")
    }

    fun uninstallHook(ip: Set<String>) {
        if (ip.isEmpty()) {
            logger.warn("uninstallHook|ip: $ip")
        }
        val req = RequestBody(
            name = "钩子卸载",
            constants = mapOf(
                "\${vm_ip}" to ip.joinToString(","),
                "\${operation_type}" to "uninstall"
            )
        )
        val body = JsonUtil.toJson(req, false)
        logger.info("uninstallHook|request url: ${bkConfig.bksopsCreateTask}, body: $body")
        val request = Request.Builder()
            .url(bkConfig.bksopsCreateTask)
            .headers(headers())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = OkhttpUtils.doHttp(request).resolveResponse<Response>()
        logger.info("uninstallHook|task status: ${resp.result}|task url: ${resp.data?.taskUrl}")
    }

    private fun headers() = mapOf(
        "X-Bkapi-Authorization" to """{"bk_app_code":"${bkConfig.appCode}","bk_app_secret":"${bkConfig.appSecret}"}"""
    ).toHeaders()

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            if (!this.isSuccessful) {
                logger.error("request api[${this.request.url.toUrl()}] error: ${this.code}, body: $responseContent")
                throw RemoteServiceException("request api[${this.request.url.toUrl()}] error", this.code)
            }

            val responseData = try {
                objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            } catch (e: Exception) {
                logger.error("request api[${this.request.url.toUrl()}] error: ${this.code}, body: $responseContent")
                throw RemoteServiceException("parse api[${this.request.url.toUrl()}] resp $responseContent", this.code)
            }

            return responseData
        }
    }
}
