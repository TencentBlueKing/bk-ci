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

package com.tencent.devops.environment.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.api.service.ServiceMonitorSpaceResource
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.BkBizProjectDao
import com.tencent.devops.environment.pojo.BizProjectItem
import com.tencent.devops.environment.pojo.BkBizProjectLock
import com.tencent.devops.environment.pojo.BkMetadataResp
import com.tencent.devops.environment.pojo.EnableDashboardResp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class BkBizProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val bizProjectDao: BkBizProjectDao,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val client: Client
) {
    @Value("\${bkMonitor.gateway:#{null}}")
    val bkMonitorGateway: String? = null

    @Value("\${bkMonitor.appCode:#{null}}")
    val bkMonitorAppCode: String? = null

    @Value("\${bkMonitor.appSecret:#{null}}")
    val bkMonitorAppSecret: String? = null

    fun addBizProjects(
        bizProjects: List<BizProjectItem>
    ): Boolean {
        return if (bizProjects.isEmpty()) {
            false
        } else if (bizProjects.size == 1) {
            bizProjectDao.add(
                dslContext = dslContext,
                bizId = bizProjects.first().bkBizId,
                projectId = bizProjects.first().projectId
            )
        } else {
            bizProjectDao.batchAdd(
                dslContext,
                bizProjects
            )
        }
    }

    fun deleteBizProject(
        id: Long
    ): Boolean {
        return bizProjectDao.delete(dslContext, id)
    }

    fun checkEnableDashboard(
        projectId: String
    ): EnableDashboardResp {
        val record = bizProjectDao.fetchRecord(dslContext, projectId)
            ?: return EnableDashboardResp(false, null)
        if (record.enableMonitorDashboard == true) {
            return if (record.bizId <= 0) {
                EnableDashboardResp(true, record.bizId)
            } else {
                // 兼容最早的一批旧数据
                EnableDashboardResp(true, -record.bizId)
            }
        }
        return EnableDashboardResp(false, null)
    }

    fun enableDashBoard(
        userId: String,
        projectId: String
    ): EnableDashboardResp {
        var bizId = getBizId(projectId)
        // 如果直接获取没有获取到说明需要开启下空间和权限
        bizId = if (bizId == null) {
            val res = client.get(ServiceMonitorSpaceResource::class).migrateMonitorResource(
                projectCodes = listOf(projectId),
                asyncMigrateOtherGroup = false
            ).data
            if (res == true) {
                val id = client.get(ServiceMonitorSpaceResource::class)
                    .getMonitorSpaceBizId(userId, projectId).data?.toLong() ?: return EnableDashboardResp(
                    false, null
                )
                // 拿到新的后直接写入
                val bizLock = BkBizProjectLock(
                    redisOperation = redisOperation,
                    projectId = projectId
                )
                try {
                    bizLock.lock()
                    val oldId = bizProjectDao.fetchBizId(dslContext, projectId)
                    if (oldId == null) {
                        bizProjectDao.add(dslContext, id, projectId)
                    }
                } catch (e: Exception) {
                    logger.error("enableDashBoard|write bizId error", e)
                } finally {
                    bizLock.unlock()
                }
                id
            } else {
                logger.warn("enableDashBoard|migrateMonitorResource false")
                return EnableDashboardResp(false, null)
            }
        } else {
            if (bizId <= 0) {
                bizId
            } else {
                // 兼容最早的一批旧数据
                -bizId
            }
        }
        quickImportDashboard(bizId)
        bizProjectDao.updateDashboard(dslContext, projectId, true)
        return EnableDashboardResp(true, bizId)
    }

    fun getBizId(projectId: String): Long? {
        var bizId = bizProjectDao.fetchBizId(dslContext, projectId)
        if (bizId != null) {
            return bizId
        }
        // 因为同时会有四个方法进行查询，所以加锁写入
        val bizLock = BkBizProjectLock(
            redisOperation = redisOperation,
            projectId = projectId
        )
        try {
            bizLock.lock()
            // 进来后再查询下，因为会有其他同时强锁的写入
            bizId = bizProjectDao.fetchBizId(dslContext, projectId)
            if (bizId != null) {
                return bizId
            }
            // 没有就拿取新的
            val url = "$bkMonitorGateway/metadata_get_space_detail?space_uid=bkci__$projectId"
            val headerStr = objectMapper.writeValueAsString(
                mapOf("bk_app_code" to bkMonitorAppCode, "bk_app_secret" to bkMonitorAppSecret)
            ).replace("\\s".toRegex(), "")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Bkapi-Authorization", headerStr)
                .build()

            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    logger.warn("request failed, uri:($url)|response: ($it)")
                    throw RemoteServiceException("getBizId failed, response:($it)")
                }
                val responseStr = it.body!!.string()
                val resp = objectMapper.readValue<BkMetadataResp>(responseStr)
                if (resp.code != 200L || !resp.result) {
                    // 请求错误
                    logger.warn("request failed, url:($url)|response:($it)")
                    throw RemoteServiceException("getBizId failed, response:(${resp.message})")
                }
                logger.debug("request response：${objectMapper.writeValueAsString(resp.data)}")
                bizId = resp.data?.id
                if (bizId == null) {
                    logger.error("request bk mate data is null")
                    return null
                }

                bizProjectDao.add(dslContext, bizId!!, projectId)
                return bizId
            }
        } catch (e: Exception) {
            logger.error("get bizId error", e)
            return null
        } finally {
            bizLock.unlock()
        }
    }

    private fun quickImportDashboard(
        bizId: Long
    ) {
        val url = "$bkMonitorGateway/quick_import_dashboard/"
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to bkMonitorAppCode, "bk_app_secret" to bkMonitorAppSecret)
        ).replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(QuickImportDashboardReq(bkBizId = bizId))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Bkapi-Authorization", headerStr)
            .build()

        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("quickImportDashboard request failed, uri:($url)|response: ($responseStr)")
                    throw RemoteServiceException("quickImportDashboard failed, response:($responseStr)")
                }
                val resp = objectMapper.readValue<QuickImportDashboardResp>(responseStr)
                if (resp.code != 200L || !resp.result) {
                    logger.warn("quickImportDashboard request failed, url:($url)|response:($responseStr)")
                    throw RemoteServiceException("quickImportDashboard failed, response:(${resp.message})")
                }
                return
            }
        } catch (e: Exception) {
            logger.warn("quickImportDashboard request failed", e)
        }
    }

    fun updateDashboard(
        projectId: String,
        updateDashboard: Boolean
    ): Boolean {
        return bizProjectDao.updateDashboard(
            dslContext = dslContext,
            projectId = projectId,
            update = updateDashboard
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkBizProjectService::class.java)
    }
}

data class QuickImportDashboardReq(
    @JsonProperty("bk_biz_id")
    val bkBizId: Long,
    @JsonProperty("dash_name")
    val dashName: String = "bkci/BKCI-构建机"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QuickImportDashboardResp(
    val result: Boolean,
    val code: Long,
    val message: String
)
