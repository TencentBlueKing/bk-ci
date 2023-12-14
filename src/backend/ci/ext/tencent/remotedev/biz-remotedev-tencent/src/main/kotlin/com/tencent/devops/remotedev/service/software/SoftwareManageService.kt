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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.SoftwareManageDao
import com.tencent.devops.remotedev.pojo.software.CommonArgs
import com.tencent.devops.remotedev.pojo.software.InstallSoftwareRes
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.pojo.software.SoftwareCreate
import com.tencent.devops.remotedev.pojo.software.SoftwareInfo
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import com.tencent.devops.remotedev.pojo.software.UserSoftwareInstalledRecord
import java.net.SocketTimeoutException
import javax.ws.rs.core.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
    val softwareGroupUrl = ""

    @Value("\${xingyun.install_software_url:}")
    val installSoftwareUrl = ""

    @Value("\${devopsGateway.host:#{null}}")
    val backendHost = ""

    /*请求合法性校验时使用的密钥*/
    @Value("\${externalKey:}")
    val externalKey = ""

    companion object {
        private val logger = LoggerFactory.getLogger(SoftwareManageService::class.java)
        private const val IOANAME = "IOA"
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
                    taskId = it.taskId,
                    softwareName = it.softwareName,
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
            .url(softwareGroupUrl)
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

    // 导入软件到项目中
    fun importSoftwareToProject(software: ProjectSoftware): Boolean {
        logger.info("SoftwareManageService|installSoftwareToUser|software|$software")
        return softwareManageDao.importSoftwareToProject(dslContext, software) > 0
    }

    /** 云桌面创建完成后安全初始化：安装ioa
     * ioa安装的脚步严格安装以下格式字符串，转base64后传入。
     * base64(-project_id "cmk-tke" -creator "raylzhang" -region_id "555" -inner_ip "SZ3.11.171.77.15")
     */
    fun installSystemSoftwares(
        projectId: String,
        creator: String,
        regionId: String,
        ip: String,
        workspaceName: String,
        autoAssign: Boolean? = false
    ) {
        val params = "-project_id \"$projectId\" -creator \"$workspaceName\" -region_id \"$regionId\" -inner_ip \"$ip\""
        val base64Val = Base64Util.encode(params.toByteArray())
        val systemSoftwareInfoList = softwareManageDao.getSystemSoftwareList(dslContext)
        logger.info("installSoftwareFromXingyun|systemSoftwareInfoList|$systemSoftwareInfoList|params|$params")
        if (systemSoftwareInfoList.isEmpty()) {
            return
        }
        val softwareInfoList = mutableListOf<SoftwareInfo>()
        systemSoftwareInfoList.forEach { record ->
            softwareInfoList.add(
                SoftwareInfo(
                    name = record["NAME"] as String,
                    version = record["VERSION"] as String,
                    commonArgs = CommonArgs(
                        base64 = base64Val,
                        cloudDesktopId = workspaceName
                    ).takeIf { record["NAME"] == IOANAME }
                )
            )
        }
        val callBackUrl = "$backendHost/remotedev/api/external/remotedev/software_install_callback" +
                "?type=SYSTEM&key=$externalKey&workspaceName=$workspaceName&" +
                "autoAssign=$autoAssign&projectId=$projectId&userId=$creator&x-devops-project-id=$projectId"
        installSoftwareFromXingyun(
            userId = creator,
            ip = ip.substringAfter("."),
            callBackUrl = callBackUrl,
            softwareInfoList = softwareInfoList
        )?.also {
            // 插入软件安装记录
            softwareManageDao.batchAddSystemInstalledRecords(
                dslContext = dslContext,
                tadkId = it.data.taskId,
                workspaceName = workspaceName,
                softwareInfoList = softwareInfoList
            )
        }
    }

    // 调用行云接口执行软件安装
    fun installSoftwareFromXingyun(
        userId: String,
        ip: String,
        callBackUrl: String,
        softwareInfoList: List<SoftwareInfo>
    ): InstallSoftwareRes? {
        // 先获取userId安装的软件列表，封装成SoftwareCreate
        val softwareCreate = SoftwareCreate(
            ip = ip,
            username = userId,
            softwareInfo = softwareInfoList,
            callbackUrl = callBackUrl
        )
        val body = JsonUtil.toJson(softwareCreate, false)
        logger.info("installSoftwareFromXingyun|installSoftwareUrl|$installSoftwareUrl|body|$body")
        val headerStr = ObjectMapper().writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))
            .replace("\\s".toRegex(), "")
        val request = Request.Builder()
            .url(installSoftwareUrl)
            .addHeader("x-bkapi-authorization", headerStr)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()
        return kotlin.runCatching {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info(
                    "installSoftwareFromXingyun|response code|$ip" +
                            "|${response.code}|responseContent|$responseContent"
                )
                if (!response.isSuccessful) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                        errorCode = ErrorCodeEnum.INSTALL_SOFTWARE_FAIL.errorCode
                    )
                }
                val createSoftwareRes: InstallSoftwareRes = jacksonObjectMapper().readValue(responseContent)
                logger.info("installSoftwareFromXingyun|createSoftwareRes|$createSoftwareRes")
                createSoftwareRes
            }
        }.onFailure {
            logger.error("install software from xingyun failed.", it)
        }.getOrNull()
    }

    // 添加系统软件安装记录
    fun updateSoftwareInstalledRecords(type: String, softwareList: SoftwareCallbackRes) {
        logger.info("updateSoftwareInstalledRecords|type|$type|softwareList|$softwareList")
        if (type == "SYSTEM") {
            softwareManageDao.updateSystemInstalledRecords(dslContext, softwareList)
        } else {
            softwareManageDao.updateUserInstalledRecords(dslContext, softwareList)
        }
    }
}
