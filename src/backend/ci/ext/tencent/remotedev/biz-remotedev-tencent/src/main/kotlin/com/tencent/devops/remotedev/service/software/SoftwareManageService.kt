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

package com.tencent.devops.remotedev.service.software

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.SoftwareManageDao
import com.tencent.devops.remotedev.pojo.Watermark
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import com.tencent.devops.remotedev.pojo.software.UserSoftwareInstalledRecord
import com.tencent.devops.remotedev.service.WatermarkService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import javax.ws.rs.core.Response

@Service
class SoftwareManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val softwareManageDao: SoftwareManageDao
) {
    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    @Value("\${xingyun.software_group_url:}")
    val software_group_url = ""

    companion object {
        private val logger = LoggerFactory.getLogger(SoftwareManageService::class.java)
    }

    // 获取工作空间模板
    fun getProjectSoftwareList(projectId: String): List<ProjectSoftware> {
        logger.info("SoftwareManageService|getProjectSoftwareList|projectId|$projectId")
        val result = mutableListOf<ProjectSoftware>()
        softwareManageDao.querySoftwareList(
            projectId = projectId,
            dslContext = dslContext
        ).forEach {
            result.add(
                ProjectSoftware(
                    id = it.id,
                    projectId = it.projectId,
                    name = it.name,
                    logo = it.logo,
                    version = it.version,
                    source = it.source,
                    status = it.status,
                    classification = it.classification,
                    installMethod = it.installMethod,
                    creator = it.creator
                )
            )
        }
        return result
    }

    // 安装软件至用户
    fun batchInstallSoftwareToUser(softwareList: List<UserSoftware>): Boolean {
        logger.info("SoftwareManageService|installSoftwareToUser|softwareList|$softwareList")
        softwareManageDao.batchInstallSoftwareToUser(dslContext, softwareList)
        return true
    }
    fun getUserSoftwareInstalledRecord(
        projectId: String,
        user: String?,
        workspaceName: String?,
        status: SoftwareInstallStatus?
    ): List<UserSoftwareInstalledRecord> {
        logger.info("SoftwareManageService|getUserSoftwareInstalledRecord|projectId|$projectId")
        val result = mutableListOf<UserSoftwareInstalledRecord>()
        softwareManageDao.queryUserSoftwareInstalledRecord(
            dslContext = dslContext,
            projectId = projectId,
            user = user,
            workspaceName = workspaceName,
            status = status
        ).forEach {
            result.add(
                UserSoftwareInstalledRecord(
                    projectId = it.projectId,
                    user = it.creator,
                    softwareId = it.softwareId,
                    workspaceName = it.workspaceName,
                    status = SoftwareInstallStatus.values()[it.status],
                    installTime = it.createTime.toString()
                )
            )
        }
        return result
    }

    fun getSoftwareGroupInfo(): Any {
        val headerStr = ObjectMapper().writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))
            .replace("\\s".toRegex(), "")
        val request = Request.Builder()
            .url(software_group_url)
            .addHeader("x-bkapi-authorization", headerStr)
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                if (!response.isSuccessful) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                        errorCode = ErrorCodeEnum.GET_SOFTWARE_GROUP_FAIL.errorCode
                    )
                }
                val data = JsonUtil.to(response.body!!.string(), Any::class.java)
                logger.info("getWatermark|response code|${response.code}|content|$data")
                return data
            }
        } catch (e: SocketTimeoutException) {
            logger.error("get software group failed.", e)
            // 接口超时失败
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = ErrorCodeEnum.GET_SOFTWARE_GROUP_FAIL.errorCode
            )
        }
    }
}
