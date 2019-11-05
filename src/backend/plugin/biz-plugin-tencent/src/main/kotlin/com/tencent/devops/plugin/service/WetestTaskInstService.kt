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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.plugin.tables.records.TPluginWetestTaskInstRecord
import com.tencent.devops.plugin.api.UserWetestTaskInstResource
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.dao.WetestTaskInstDao
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInstReport
import com.tencent.devops.plugin.utils.CommonUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.jooq.Result
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestTaskInstService @Autowired constructor(
    private val wetestTaskInstDao: WetestTaskInstDao,
    private val client: Client,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WeTestClient::class.java)
    }

    fun getTaskInstReportByBuildId(projectId: String, pipelineId: String?, buildId: String?, page: Int?, pageSize: Int?): Page<WetestTaskInstReport> {
        var offset: Int? = null
        var limit: Int? = null
        if (page != null && pageSize != null) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            offset = sqlLimit.offset
            limit = sqlLimit.limit
        }
        val records = wetestTaskInstDao.getTaskInstByBuildId(dslContext, projectId, pipelineId, buildId, offset, limit)?.map {
            val pipelineName = client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, setOf(it.pipelineId)).data?.get(it.pipelineId)
            WetestTaskInstReport(
                    it.testId,
                    it.projectId,
                    it.pipelineId,
                    pipelineName ?: "",
                    it.buildId,
                    it.buildNo,
                    it.name,
                    it.version,
                    it.passingRate,
                    it.taskId.toString(),
                    it.testType,
                    it.scriptType,
                    it.isSync,
                    it.ticketId ?: "",
                    it.startUser,
                    it.status,
                    it.beginTime.timestampmilli(),
                    it.endTime?.timestampmilli()
            )
        } ?: listOf()
        val count = wetestTaskInstDao.getTaskInstCountByBuildId(dslContext, projectId, pipelineId, buildId)
        return Page(page ?: 1, pageSize ?: count, count.toLong(), records)
    }

    fun getTaskInstReportByPage(projectId: String, pipelineId: Set<String>?, version: Set<String>?, page: Int?, pageSize: Int?): Page<WetestTaskInstReport> {
        var offset: Int? = null
        var limit: Int? = null
        if (page != null && pageSize != null) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            offset = sqlLimit.offset
            limit = sqlLimit.limit
        }
        val records = wetestTaskInstDao.getTaskInst(dslContext, projectId, pipelineId, version, offset, limit)?.map {
            val pipelineName = client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, setOf(it.pipelineId)).data?.get(it.pipelineId)
            WetestTaskInstReport(
                    it.testId,
                    it.projectId,
                    it.pipelineId,
                    pipelineName ?: "",
                    it.buildId,
                    it.buildNo,
                    it.name,
                    it.version,
                    it.passingRate,
                    it.taskId.toString(),
                    it.testType,
                    it.scriptType,
                    it.isSync,
                    it.ticketId ?: "",
                    it.startUser,
                    it.status,
                    it.beginTime.timestampmilli(),
                    it.endTime?.timestampmilli()
            )
        } ?: listOf()
        val count = wetestTaskInstDao.getTaskInstCount(dslContext, projectId, pipelineId, version)
        return Page(page ?: 1, pageSize ?: count, count.toLong(), records)
    }

    fun getUnfinishTask(): Result<TPluginWetestTaskInstRecord>? {
        return wetestTaskInstDao.getUnfinishTask(dslContext)
    }

    fun saveTask(wetestTaskInst: WetestTaskInst): String {
        return wetestTaskInstDao.insert(dslContext, wetestTaskInst).toString()
    }

    fun updateTaskInstStatus(testId: String, status: WetestInstStatus, passRate: String? = null): String {
        return wetestTaskInstDao.updateTaskInstStatus(dslContext, testId, status, passRate).toString()
    }

    fun getTaskInstVersion(projectId: String, pipelineId: Set<String>?): Set<String> {
        val versionSet = wetestTaskInstDao.getTaskInst(dslContext, projectId, pipelineId)?.map { it.version }?.toSet()
                ?: setOf()
        return versionSet.filter { !it.isNullOrBlank() }.toSet()
    }

    fun listPipeline(projectId: String, version: Set<String>?): List<UserWetestTaskInstResource.PipelineData> {
        val pipelineIds = wetestTaskInstDao.getTaskInst(dslContext, projectId, null, version)?.map { it.pipelineId }
                ?: listOf()
        return client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIds.toSet()).data?.map {
            UserWetestTaskInstResource.PipelineData(it.key, it.value)
        } ?: listOf()
    }

    fun getSession(userId: String): Map<String, Any> {
        val (secretId, secretKey) = CommonUtils.getCredential(userId)

        val client = WeTestClient(secretId, secretKey)
        val response = client.getSession(userId)

        if (response.has("flag") && response.getBoolean("flag")) {
            val session = (response["ret"] as JSONObject).toMap()
            val sessionid = session["sessionid"] as String
            val sessionkey = session.getOrDefault("sessionkey", "openqa_sessionid")
            return mapOf("sessionid" to sessionid, "sessionkey" to sessionkey)
        }
        if (response.has("ret") && response.getInt("ret") != 0) {
            val msg = response.optString("msg")
            logger.error("fail to get getWetestSession from weTest, retCode: ${response.getInt("ret")}, msg: $msg")
            return mapOf("ret" to -1, "error" to msg)
        }
        return mapOf("ret" to -1, "error" to "获取wetest session失败，response：$response")
    }
}