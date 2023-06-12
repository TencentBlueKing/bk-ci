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

package com.tencent.devops.auth.service.migrate

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.dto.response.ResponseDTO
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResp
import com.tencent.devops.auth.pojo.migrate.MigrateTaskIdResp
import com.tencent.devops.auth.pojo.migrate.MigrateTaskStatusResp
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class MigrateIamApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateIamApiService::class.java)

        private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        // 启动iam v3迁移任务
        private const val V3_IAM_MIGRATE_TASK = "api/v2/open/migration/bkci/task/"

        // 启动iam v0迁移任务
        private const val V0_IAM_MIGRATE_TASK = "api/v2/open/migration/bkci/legacy_task/"

        // 获取iam迁移数据
        private const val IAM_GET_MIGRATE_DATA = "api/v2/open/migration/bkci/data/"

        // 迁移成功状态
        const val SUCCESSFUL_IAM_MIGRATE_TASK_SUCCESS = "SUCCESS"

        // 轮询获取iam迁移状态睡眠时间
        const val SLEEP_LOOP_IAM_GET_MIGRATE_TASK = 30000L

        // ci通过iam接口创建的用户组
        const val GROUP_API_POLICY = "group_api_policy"

        // 用户在iam界面创建的用户组
        const val GROUP_WEB_POLICY = "group_web_policy"

        // 用户自定义权限
        const val USER_CUSTOM_POLICY = "user_custom_policy"
    }

    // iam迁移的token
    @Value("\${auth.migrateToken:#{null}}")
    private val migrateIamToken: String = ""

    @Value("\${auth.webHost:}")
    private val iamWebHost = ""

    /**
     * 启动v3迁移任务
     *
     * @param v3GradeManagerIds v3分级管理员ID
     */
    fun startV3MigrateTask(v3GradeManagerIds: List<String>) {
        logger.info("start v3 migrate task $v3GradeManagerIds")
        val data = JsonUtil.toJson(v3GradeManagerIds).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$iamWebHost/$V3_IAM_MIGRATE_TASK?token=$migrateIamToken")
            .post(data)
            .build()
        getBody(operation = "failed to start v3 migrate task", request = request)
    }

    fun getV3MigrateTaskStatus(): String {
        val request = Request.Builder()
            .url("$iamWebHost/$V3_IAM_MIGRATE_TASK?token=$migrateIamToken")
            .get()
            .build()
        return JsonUtil.to(
            getBody("get iam v3 migrate task status", request),
            object : TypeReference<ResponseDTO<MigrateTaskStatusResp>>() {}
        ).data.status
    }

    /**
     * 启动v0迁移任务
     *
     * @param projectCodes v0项目ID
     */
    fun startV0MigrateTask(projectCodes: List<String>): Int {
        logger.info("start v3 migrate task $projectCodes")
        val data = JsonUtil.toJson(projectCodes).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$iamWebHost/$V0_IAM_MIGRATE_TASK?token=$migrateIamToken")
            .post(data)
            .build()
        return JsonUtil.to(
            getBody(operation = "failed to start v0 migrate task", request = request),
            object : TypeReference<ResponseDTO<MigrateTaskIdResp>>() {}
        ).data.id
    }

    fun getV0MigrateTaskStatus(migrateTaskId: Int): String {
        val request = Request.Builder()
            .url("$iamWebHost/$V0_IAM_MIGRATE_TASK$migrateTaskId/?token=$migrateIamToken")
            .get()
            .build()
        return JsonUtil.to(
            getBody("get iam v3 migrate task status", request),
            object : TypeReference<ResponseDTO<MigrateTaskStatusResp>>() {}
        ).data.status
    }

    /**
     * 获取迁移数据
     *
     * @param projectCode 项目ID
     * @param migrateType 迁移类型,group_api_policy、group_web_policy、user_custom_policy
     */
    fun getMigrateData(
        projectCode: String,
        migrateType: String,
        version: String,
        page: Int,
        pageSize: Int
    ): MigrateTaskDataResp {
        val request = Request.Builder()
            .url(
                "$iamWebHost/$IAM_GET_MIGRATE_DATA?" +
                    "token=$migrateIamToken&project_id=$projectCode&type=$migrateType" +
                    "&version=$version&page=$page&page_size=$pageSize"
            )
            .get()
            .build()
        return JsonUtil.to(
            getBody("get iam migrate data", request),
            object : TypeReference<ResponseDTO<MigrateTaskDataResp>>() {}
        ).data
    }

    private fun getBody(operation: String, request: Request): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("request iam migrate api ${request.url} response|$responseContent")
            if (!response.isSuccessful) {
                logger.warn("Failed to request(${request.url}), code ${response.code}, content: $responseContent")
                throw RemoteServiceException(operation)
            }
            return responseContent
        }
    }
}
