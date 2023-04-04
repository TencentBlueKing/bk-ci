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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.constant.I18NConstant.BK_APP_SCAN_COMPLETED
import com.tencent.devops.common.api.constant.I18NConstant.BK_VIEW_DETAILS
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.plugin.tables.TPluginJingang
import com.tencent.devops.model.plugin.tables.TPluginJingangResult
import com.tencent.devops.plugin.dao.JinGangAppDao
import com.tencent.devops.plugin.dao.JinGangAppMetaDao
import com.tencent.devops.plugin.pojo.JinGangApp
import com.tencent.devops.plugin.pojo.JinGangAppCallback
import com.tencent.devops.plugin.pojo.JinGangAppResultReponse
import com.tencent.devops.process.api.service.ServiceJfrogResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import javassist.NotFoundException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.json.XML
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate

@Service
@Suppress(
    "SwallowedException", "TooGenericExceptionCaught",
    "TooManyFunctions", "LongParameterList", "MagicNumber", "ComplexMethod", "LongMethod", "ThrowsCount"
)
class JinGangService @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val jinGangAppDao: JinGangAppDao,
    private val jinGangAppMetaDao: JinGangAppMetaDao,
    private val authPermissionApi: BSAuthPermissionApi,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val authProjectApi: AuthProjectApi,
    private val pipelineServiceCode: PipelineAuthServiceCode,
    private val bkRepoClient: BkRepoClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JinGangService::class.java)
    }

    @Value("\${gateway.url}")
    private lateinit var gatewayUrl: String

    @Value("\${jinGang.url}")
    private lateinit var jinGangUrl: String

    @Value("\${star.url}")
    private lateinit var starUrl: String

    fun callback(data: JinGangAppCallback) {
        logger.info("jin gang callback>>> $data")
        if (data.status == "0") {
            val xml = downloadXml(data.scanXml)
            val resultJson = convertXml(xml)
            jinGangAppDao.updateTask(
                dslContext,
                data.buildId,
                data.md5,
                data.status.toInt(),
                data.taskId.toLong(),
                data.scanUrl,
                resultJson
            )

            buildLogPrinter.addLine(
                buildId = data.buildId,
                message = MessageUtil.getMessageByLocale(
                    messageCode = BK_APP_SCAN_COMPLETED,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + "【<a target='_blank' href='${data.scanUrl}'>" + MessageUtil.getMessageByLocale(
                    messageCode = BK_VIEW_DETAILS,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + "</a>】",
                tag = data.elementId,
                jobId = "",
                executeCount = 1
            )
        } else {
            jinGangAppDao.updateTask(
                dslContext,
                data.buildId,
                data.md5,
                data.status.toInt(),
                data.taskId.toLong(),
                data.scanUrl,
                data.msg
            )
        }
    }

    private fun downloadXml(scanXmlUrl: String): String {
        val request = Request.Builder()
            .url(scanXmlUrl)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            return it.body!!.string()
        }
    }

    private fun convertXml(xml: String): String {
        return XML.toJSONObject(xml).toString()
    }

    fun scanApp(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        elementId: String,
        file: String,
        isCustom: Boolean = false,
        runType: String = "1",
        checkPermission: Boolean = false
    ): String {
        logger.info("scan app: $userId, $projectId, $pipelineId, $buildId, $file")

        if (checkPermission) {
            if (!authPermissionApi.validateUserResourcePermission(
                    userId, pipelineServiceCode, AuthResourceType.PIPELINE_DEFAULT,
                    projectId, pipelineId, AuthPermission.EXECUTE
                )
            )
                throw PermissionForbiddenException("user($userId) does not has permission for pipeline: $pipelineId")
        }

        val type = if (isCustom) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE

        // 获取文件信息
        val jfrogFile = client.get(ServiceArtifactoryResource::class).show(userId, projectId, type, file).data!!
        // 扫描
        return scan(jfrogFile, isCustom, userId, runType, projectId, pipelineId, buildId, buildNo, elementId)
    }

    private fun scan(
        jfrogFile: FileDetail,
        isCustom: Boolean,
        userId: String,
        runType: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        elementId: String
    ): String {
        val fileName = File(jfrogFile.name).name
        val type = when {
            fileName.endsWith(".apk") -> 0
            fileName.endsWith(".ipa") -> 1
            else -> throw IllegalArgumentException("$fileName is not a app")
        }

        val version = jfrogFile.meta[ARCHIVE_PROPS_APP_VERSION] ?: throw IllegalArgumentException("no appVersion found")
        val bundleIdentifier = jfrogFile.meta[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER]
            ?: throw IllegalArgumentException("no bundleIdentifier found")
        val pipelineName = client.get(ServiceJfrogResource::class).getPipelineNameByIds(projectId, setOf(pipelineId))
            .data?.get(pipelineId) ?: throw IllegalArgumentException("no pipeline name found for $pipelineId")
        val shareUri = bkRepoClient.createShareUri(
            creatorId = userId,
            projectId = projectId,
            repoName = if (isCustom) "custom" else "pipeline",
            fullPath = jfrogFile.fullPath,
            downloadUsers = listOf(),
            downloadIps = listOf(),
            timeoutInSeconds = 3600 * 24
        )
        val fileUrl = "http://$gatewayUrl/bkrepo/api/user/repository$shareUri"
        val projectInfo =
            client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data?.firstOrNull()
        val ccId = projectInfo?.ccAppId ?: throw IllegalArgumentException("no ccid found for project: $projectId")
        val starResponse = getStarResponse(projectId, ccId.toString())
        val releaseType = if (starResponse.status == "正在运行") "1" else "0" // 0表示游戏还未上线，1表示该游戏已上线
        val submitUser = starResponse.user.firstOrNull { it.roleId == "37" }?.user ?: ""

        // 记录该次扫描
        val taskId = jinGangAppDao.createTask(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildNo = buildNo,
            userId = userId,
            path = jfrogFile.name,
            md5 = jfrogFile.checksums.md5,
            size = jfrogFile.size,
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis(),
            version = version,
            type = type
        )

        try {
            val params = mutableMapOf<String, String>()
            params["productName"] = pipelineName
            params["fileName"] = fileName
            params["fileUrl"] = fileUrl
            params["fileMd5"] = jfrogFile.checksums.md5
            params["versionNumber"] = version
            params["packagename"] = bundleIdentifier
            params["releasetime"] = LocalDate.now().toString().replace("-", "")
            params["buildId"] = buildId
            params["pipelineId"] = pipelineId
            params["pipelineName"] = pipelineName
            params["elementId"] = elementId
            params["pipelineUrl"] =
                "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
            params["projectId"] = projectId
            params["extension"] = type.toString()
            params["release_type"] = releaseType
            params["responseuser"] = submitUser.ifBlank { userId } // 产品负责人, 上传人
            params["submituser"] = userId // 邮件抄送人
            params["taskId"] = taskId.toString() // 任务id
            params["is_run_kingkong_v2"] = if (type == 1) "3" else runType // ios只有静态扫描
            // 任务id
            params["responseUrl"] = HomeHostUtil.innerApiHost() + "/plugin/api/external/jingang/app/callback"
            params["bg"] = getBgName(projectInfo.bgId?.toLong())
            val json = objectMapper.writeValueAsString(params)
            logger.info("jin gang request json:>>>> $json")

            val request = Request.Builder()
                .url(jinGangUrl)
                .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), json))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val respJson = response.body!!.string()
                logger.info("jin gang response: $respJson")
                val obj: Map<String, Any> = JsonUtil.toMap(respJson)
                if (obj["status"]?.toString() != "1") {
                    throw RemoteServiceException(httpStatus = 400, errorMessage = "fail to start app scan:$respJson")
                } else {
                    logger.info("jin gang app scan successfully")
                }
                jinGangAppMetaDao.incRunCount(dslContext, projectId, pipelineId)
                return taskId.toString()
            }
        } catch (e: Exception) {
            jinGangAppDao.updateTask(
                dslContext = dslContext,
                buildId = buildId,
                md5 = jfrogFile.checksums.md5,
                status = -1,
                taskId = taskId,
                scanUrl = "",
                result = e.message ?: ""
            )
            throw e
        }
    }

    private fun getBgName(bgId: Long?): String {
        return when (bgId) {
            956L -> "IEG"
            953L -> "CDG"
            29294L -> "CSIG"
            14129L -> "WXG"
            29292L -> "PCG"
            958L -> "TEG"
            else -> "OTHER"
        }
    }

    private fun getStarResponse(projectId: String, ccId: String): StarResponse {
        logger.info("star ccid: $ccId")
        if (ccId == "0") {
            return StarResponse("0", getProjectManager(projectId))
        }

        val request = Request.Builder().url("$starUrl?id=$ccId").get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val json = response.body!!.string()

            logger.info("star response: $json")
            val obj: Map<String, Any> = JsonUtil.toMap(json)
            val status = obj["status"]?.toString()
            if (status == "1") {
                @Suppress("UNCHECKED_CAST")
                val data = obj["data"] as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val user = data["user"] as Collection<Any>?
                return StarResponse(
                    status = data["status"].toString(),
                    user = user?.map {
                        @Suppress("UNCHECKED_CAST")
                        val item = it as Map<String, Any>
                        StarUser(
                            roleName = item["roleName"]?.toString() ?: "",
                            roleId = item["roleId"]?.toString() ?: "",
                            user = item["user"]?.toString() ?: ""
                        )
                    } ?: listOf()
                )
            } else {
                throw RemoteServiceException("fail to get project from star(ccId= $ccId): $json")
            }
        }
    }

    private fun getProjectManager(projectId: String): List<StarUser> {
        val manager = authProjectApi.getProjectUsers(pipelineServiceCode, projectId, BkAuthGroup.MANAGER)
        return listOf(StarUser(roleName = "项目管理员", roleId = "37", user = manager.joinToString(";")))
    }

    data class StarResponse(
        val status: String,
        val user: List<StarUser>
    )

    data class StarUser(
        val roleName: String,
        val roleId: String,
        val user: String
    )

    fun getList(projectId: String, page: Int, pageSize: Int): List<JinGangApp> {
        val recordList = jinGangAppDao.getList(dslContext, projectId, page, pageSize)
        val result = mutableListOf<JinGangApp>()
        if (recordList != null) {
            with(TPluginJingang.T_PLUGIN_JINGANG) {
                val pipelineIds = mutableSetOf<String>()

                for (item in recordList) {
                    pipelineIds.add(item.get(PIPELINE_ID))
                }
                val pipelineNames =
                    client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIds).data
                        ?: throw NotFoundException("no pipeline name found for $pipelineIds")
                for (item in recordList) {
                    result.add(
                        JinGangApp(
                            id = item.get(ID),
                            projectId = item.get(PROJECT_ID),
                            pipelineId = item.get(PIPELINE_ID),
                            pipelineName = pipelineNames[item.get(PIPELINE_ID)] ?: "",
                            buildId = item.get(BUILD_ID),
                            buildNo = item.get(BUILD_NO),
                            fileName = covertJinGangFilePath(item.get(FILE_PATH)),
                            fileMD5 = item.get(FILE_MD5),
                            fileSize = item.get(FILE_SIZE),
                            createTime = item.get(CREATE_TIME).timestampmilli(),
                            updateTime = item.get(UPDATE_TIME).timestampmilli(),
                            creator = item.get(USER_ID),
                            status = covertJinGangStatus(item.get(STATUS)),
                            type = covertJinGangType(item.get(TYPE)),
                            version = item.get(VERSION)
                        )
                    )
                }
            }
        }
        return result
    }

    fun getCount(projectId: String): Int {
        return jinGangAppDao.getCount(dslContext, projectId)
    }

    fun getAppResult(userId: String, taskId: Long): JinGangAppResultReponse? {

        val recordResult = jinGangAppDao.getTaskResult(dslContext, taskId)
        val recordTask = jinGangAppDao.getTask(dslContext, taskId)

        // 权限校验
        val projectId = recordTask?.projectId ?: ""
        if (!authProjectApi.getUserProjects(pipelineServiceCode, userId, null).contains(projectId)) {
            throw PermissionForbiddenException("user($userId) does not has permission for project: $projectId")
        }

        var fileName = ""
        var version = ""
        var scanUrl = ""
        var responseuser = ""
        if (recordTask != null) {
            with(TPluginJingang.T_PLUGIN_JINGANG) {
                fileName = covertJinGangFilePath(recordTask.get(FILE_PATH))
                version = recordTask.get(VERSION)
                scanUrl = recordTask.get(SCAN_URL) ?: ""
                responseuser = recordTask.get(USER_ID)
            }
        }
        if (recordResult != null) {
            with(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT) {
                return JinGangAppResultReponse(
                    id = recordResult.get(ID),
                    buildId = recordResult.get(BUILD_ID),
                    fileMD5 = recordResult.get(FILE_MD5),
                    result = objectMapper.readValue(recordResult.get(RESULT)),
                    taskId = recordResult.get(TASK_ID),
                    fileName = fileName,
                    version = version,
                    scanUrl = scanUrl,
                    responseuser = responseuser
                )
            }
        } else {
            return null
        }
    }

    private fun covertJinGangType(type: Int?): String {
        return when (type) {
            0 -> "android"
            1 -> "ios"
            else -> "其他"
        }
    }

    private fun covertJinGangStatus(status: Int?): String {
        return when (status) {
            0 -> "成功"
            null -> "扫描中"
            else -> "失败"
        }
    }

    private fun covertJinGangFilePath(filePath: String?): String {
        return if (filePath != null) {
            val newFilePath = filePath.trim()
            newFilePath.substring(newFilePath.lastIndexOf("/") + 1)
        } else {
            ""
        }
    }

    fun createTask(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        userId: String,
        path: String,
        md5: String,
        size: Long,
        version: String,
        type: Int
    ): Long {
        // 记录该次扫描
        val taskId = jinGangAppDao.createTask(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildNo = buildNo,
            userId = userId,
            path = path,
            md5 = md5,
            size = size,
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis(),
            version = version,
            type = type
        )
        jinGangAppMetaDao.incRunCount(dslContext = dslContext, projectId = projectId, pipelineId = pipelineId)
        return taskId
    }

    fun updateTask(buildId: String, md5: String, status: Int, taskId: Long, scanUrl: String, result: String) {
        jinGangAppDao.updateTask(
            dslContext = dslContext,
            buildId = buildId,
            md5 = md5,
            status = status,
            taskId = taskId,
            scanUrl = scanUrl,
            result = result
        )
    }
}
