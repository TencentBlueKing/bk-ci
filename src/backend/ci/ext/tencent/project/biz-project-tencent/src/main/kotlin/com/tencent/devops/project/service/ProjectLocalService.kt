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

package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_CREATE
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_UPDATE
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_UPDATE_LOGO
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_CREATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_UPDATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_UPDATE_LOGO
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.PaasCCCreateProject
import com.tencent.devops.project.pojo.PaasCCUpdateProject
import com.tencent.devops.project.pojo.PaasCCUpdateProjectLogo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.service.job.SynProjectService.Companion.ENGLISH_NAME_PATTERN
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ImageUtil.drawImage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.ArrayList
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException

@Service
class ProjectLocalService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val rabbitTemplate: RabbitTemplate,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper,
    private val tofService: TOFService,
    private val redisOperation: RedisOperation,
    private val bkAuthProjectApi: BSAuthProjectApi,
    private val bkAuthProperties: BkAuthProperties,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val gray: Gray,
    private val jmxApi: ProjectJmxApi
) {

    private var authUrl: String = "${bkAuthProperties.url}/projects"

    /**
     * 创建项目信息
     */
    fun create(userId: String, accessToken: String, projectCreateInfo: ProjectCreateInfo): String {
        validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
        validate(ProjectValidateType.english_name, projectCreateInfo.englishName)

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            // 随机生成图片
            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)

                // 创建AUTH项目
                val authUrl = "$authUrl?access_token=$accessToken"
                val param: MutableMap<String, String> = mutableMapOf("project_code" to projectCreateInfo.englishName)
                val mediaType = MediaType.parse("application/json; charset=utf-8")
                val json = objectMapper.writeValueAsString(param)
                val requestBody = RequestBody.create(mediaType, json)
                val request = Request.Builder().url(authUrl).post(requestBody).build()
                val responseContent =
                    request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.CALL_PEM_FAIL))
                val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
                if (result.isNotOk()) {
                    logger.warn("Fail to create the project of response $responseContent")
                    throw OperationException(
                        MessageCodeUtil.generateResponseDataObject<String>(
                            ProjectMessageCode.CALL_PEM_FAIL_PARM, arrayOf(result.message!!)
                        ).message!!
                    )
                }
                val authProjectForCreateResult = result.data
                val projectId = if (authProjectForCreateResult != null) {
                    if (authProjectForCreateResult.project_id.isBlank()) {
                        throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_FAIL))
                    }
                    authProjectForCreateResult.project_id
                } else {
                    logger.warn("Fail to get the project id from response $responseContent")
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_ID_INVALID))
                }
                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户机构信息
                try {
                    projectDao.create(
                        dslContext = dslContext,
                        userId = userId,
                        logoAddress = logoAddress,
                        projectCreateInfo = projectCreateInfo,
                        userDeptDetail = userDeptDetail,
                        projectId = projectId,
                        channelCode = ProjectChannelCode.BS
                    )
                } catch (e: DuplicateKeyException) {
                    logger.warn("Duplicate project $projectCreateInfo", e)
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
                } catch (t: Throwable) {
                    logger.warn("Fail to create the project ($projectCreateInfo)", t)
                    deleteProjectFromAuth(projectId, accessToken)
                    throw t
                }

                rabbitTemplate.convertAndSend(
                    EXCHANGE_PAASCC_PROJECT_CREATE,
                    ROUTE_PAASCC_PROJECT_CREATE, PaasCCCreateProject(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = projectId,
                        retryCount = 0,
                        projectCreateInfo = projectCreateInfo
                    )
                )
                success = true
                return projectId
            } finally {
                if (logoFile.exists()) {
                    logoFile.delete()
                }
            }
        } finally {
//            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long?,
        deptName: String?,
        centerName: String?,
        interfaceName: String?
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByOrganization(
                dslContext = dslContext,
                bgId = bgId,
                deptName = deptName,
                centerName = centerName
            )?.filter { it.enabled == null || it.enabled }?.map { it.englishName }?.toList() ?: emptyList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    fun getOrCreatePreProject(userId: String, accessToken: String): ProjectVO {
        val projectCode = "_$userId"
        var userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        if (userProjectRecord != null) {
            return packagingBean(userProjectRecord, setOf())
        }

        val projectCreateInfo = ProjectCreateInfo(
            projectName = projectCode,
            englishName = projectCode,
            projectType = ProjectTypeEnum.SUPPORT_PRODUCT.index,
            description = "prebuild project for $userId",
            bgId = 0L,
            bgName = "",
            deptId = 0L,
            deptName = "",
            centerId = 0L,
            centerName = "",
            secrecy = false,
            kind = 0
        )

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            // 随机生成图片
            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())

            var projectId = getProjectIdInAuth(projectCode, accessToken)
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)

                if (null == projectId) {
                    // 创建AUTH项目
                    val authUrl = "$authUrl?access_token=$accessToken"
                    val param: MutableMap<String, String> =
                        mutableMapOf("project_code" to projectCreateInfo.englishName)
                    val mediaType = MediaType.parse("application/json; charset=utf-8")
                    val json = objectMapper.writeValueAsString(param)
                    val requestBody = RequestBody.create(mediaType, json)
                    val request = Request.Builder().url(authUrl).post(requestBody).build()
                    val responseContent =
                        request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.CALL_PEM_FAIL))
                    val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
                    if (result.isNotOk()) {
                        logger.warn("Fail to create the project of response $responseContent")
                        throw OperationException(
                            MessageCodeUtil.generateResponseDataObject<String>(
                                ProjectMessageCode.CALL_PEM_FAIL_PARM, arrayOf(result.message!!)
                            ).message!!
                        )
                    }
                    val authProjectForCreateResult = result.data
                    projectId = if (authProjectForCreateResult != null) {
                        if (authProjectForCreateResult.project_id.isBlank()) {
                            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_ID_INVALID))
                        }
                        authProjectForCreateResult.project_id
                    } else {
                        logger.warn("Fail to get the project id from response $responseContent")
                        throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_ID_INVALID))
                    }
                }
                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户机构信息                try {
                try {
                projectDao.create(
                    dslContext = dslContext,
                    userId = userId,
                    logoAddress = logoAddress,
                    projectCreateInfo = projectCreateInfo,
                    userDeptDetail = userDeptDetail,
                    projectId = projectId,
                    channelCode = ProjectChannelCode.BS
                )
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectCreateInfo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
            } catch (t: Throwable) {
                logger.warn("Fail to create the project ($projectCreateInfo)", t)
                deleteProjectFromAuth(projectId, accessToken)
                throw t
            }

                rabbitTemplate.convertAndSend(
                    EXCHANGE_PAASCC_PROJECT_CREATE,
                    ROUTE_PAASCC_PROJECT_CREATE, PaasCCCreateProject(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = projectId,
                        retryCount = 0,
                        projectCreateInfo = projectCreateInfo
                    )
                )
                success = true
            } finally {
                if (logoFile.exists()) {
                    logoFile.delete()
                }
            }
        } finally {
            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }

        userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        return packagingBean(userProjectRecord!!, setOf())
    }

    fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val grayProjectSet = grayProjectSet()
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroup(dslContext, bgName, deptName, centerName).filter { it.enabled == null || it.enabled }
                .map {
                    list.add(packagingBean(it, grayProjectSet))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun updateUsableStatus(userId: String, englishName: String, enabled: Boolean) {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectId = projectDao.getByEnglishName(dslContext, englishName)?.projectId
                ?: throw NotFoundException("项目 - $englishName 不存在")

            logger.info("[$userId|$projectId|$enabled] Start to update project usable status")
            if (bkAuthProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER).contains(
                    userId
                )
            ) {
                val updateCnt = projectDao.updateUsableStatus(dslContext, userId, projectId, enabled)
                if (updateCnt != 1) {
                    logger.warn("更新数据库出错，变更行数为:$updateCnt")
                }
            } else {
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
            }
            logger.info("[$userId|[$projectId] Project usable status is changed to $enabled")
            success = true
        } finally {
            jmxApi.execute(PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    fun getByEnglishName(accessToken: String, englishName: String): ProjectVO {
        val projectVO = getByEnglishName(englishName)
        val projectAuthIds = getAuthProjectIds(accessToken)
        if (!projectAuthIds.contains(projectVO!!.projectId)) {
            logger.warn("The user don't have the permission to get the project $englishName")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))
        }
        return projectVO
    }

    fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return packagingBean(record, grayProjectSet())
    }

    fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        logger.info("getProjectUsers accessToken is :$accessToken,userId is :$userId,projectCode is :$projectCode")
        // 检查用户是否有查询项目下用户列表的权限
        val validateResult = verifyUserProjectPermission(accessToken, projectCode, userId)
        logger.info("getProjectUsers validateResult is :$validateResult")
        val validateFlag = validateResult.data
        if (null == validateFlag || !validateFlag) {
            val messageResult = MessageCodeUtil.generateResponseDataObject<String>(CommonMessageCode.PERMISSION_DENIED)
            return Result(messageResult.status, messageResult.message, null)
        }
        val projectUserList = bkAuthProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectCode)
        logger.info("getProjectUsers projectUserList is :$projectUserList")
        return Result(projectUserList)
    }

    fun getProjectUserRoles(
        accessToken: String,
        userId: String,
        projectCode: String,
        serviceCode: AuthServiceCode
    ): List<UserRole> {
        val groupAndUsersList = bkAuthProjectApi.getProjectGroupAndUserList(serviceCode, projectCode)
        return groupAndUsersList.filter { it.userIdList.contains(userId) }
            .map { UserRole(it.displayName, it.roleId, it.roleName, it.type) }
    }

    fun update(userId: String, accessToken: String, englishName: String, projectUpdateInfo: ProjectUpdateInfo) {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val appName = if (projectUpdateInfo.ccAppId != null && projectUpdateInfo.ccAppId!! > 0) {
                tofService.getCCAppName(projectUpdateInfo.ccAppId!!)
            } else {
                null
            }
            val projectId = projectDao.getByEnglishName(dslContext, englishName)?.projectId
                ?: throw NotFoundException("项目 - $englishName 不存在")

            projectUpdateInfo.ccAppName = appName
            projectDao.update(dslContext, userId, projectId, projectUpdateInfo)
            rabbitTemplate.convertAndSend(
                EXCHANGE_PAASCC_PROJECT_UPDATE,
                ROUTE_PAASCC_PROJECT_UPDATE, PaasCCUpdateProject(
                    userId = userId,
                    accessToken = accessToken,
                    projectId = projectId,
                    retryCount = 0,
                    projectUpdateInfo = projectUpdateInfo
                )
            )
            success = true
        } catch (e: DuplicateKeyException) {
            logger.warn("Duplicate project $projectUpdateInfo", e)
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
        } finally {
            jmxApi.execute(PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    fun updateLogo(
        userId: String,
        accessToken: String,
        englishName: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<ProjectLogo> {
        logger.info("Update the logo of project $englishName")
        val project = projectDao.getByEnglishName(dslContext, englishName)
        if (project != null) {
            var logoFile: File? = null
            try {
                logoFile = convertFile(inputStream)
                val logoAddress = s3Service.saveLogo(logoFile, project.englishName)
                projectDao.updateLogoAddress(dslContext, userId, project.projectId, logoAddress)
                rabbitTemplate.convertAndSend(
                    EXCHANGE_PAASCC_PROJECT_UPDATE_LOGO,
                    ROUTE_PAASCC_PROJECT_UPDATE_LOGO, PaasCCUpdateProjectLogo(
                        userId = userId,
                        accessToken = accessToken,
                        projectId = project.projectId,
                        retryCount = 0,
                        projectUpdateLogoInfo = ProjectUpdateLogoInfo(logoAddress, userId)
                    )
                )
                return Result(ProjectLogo(logoAddress))
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.UPDATE_LOGO_FAIL))
            } finally {
                logoFile?.delete()
            }
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.QUERY_PROJECT_FAIL))
        }
    }

    fun list(accessToken: String, includeDisable: Boolean?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectIdList = getAuthProjectIds(accessToken).toSet()
            val list = ArrayList<ProjectVO>()

            val grayProjectSet = grayProjectSet()

            projectDao.list(dslContext, projectIdList).filter {
                includeDisable == true || it.enabled == null || it.enabled
            }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    fun verifyUserProjectPermission(accessToken: String, projectCode: String, userId: String): Result<Boolean> {
        val url = "$authUrl/$projectCode/users/$userId/verfiy?access_token=$accessToken"
        logger.info("the verifyUserProjectPermission url is:$url")
        val body = RequestBody.create(MediaType.parse(MessageProperties.CONTENT_TYPE_JSON), "{}")
        val request = Request.Builder().url(url).post(body).build()
        val responseContent = request(request, "verifyUserProjectPermission error")
        val result = objectMapper.readValue<Result<Any?>>(responseContent)
        logger.info("the verifyUserProjectPermission result is:$result")
        if (result.isOk()) {
            return Result(true)
        }
        return Result(false)
    }

    private fun getAuthProjectIds(accessToken: String): List<String/*projectId*/> {
        val url = "$authUrl?access_token=$accessToken"
        logger.info("Start to get auth projects - ($url)")
        val request = Request.Builder().url(url).get().build()
        val responseContent = request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        }
        if (result.data == null) {
            return emptyList()
        }

        return result.data!!.map {
            it.project_id
        }.toList()
    }

    private fun grayProjectSet() =
        (redisOperation.getSetMembers(gray.getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()

    private fun packagingBean(tProjectRecord: TProjectRecord, grayProjectSet: Set<String>): ProjectVO {
        return ProjectVO(
            id = tProjectRecord.id,
            projectId = tProjectRecord.projectId,
            projectName = tProjectRecord.projectName,
            englishName = tProjectRecord.englishName ?: "",
            projectCode = tProjectRecord.englishName ?: "",
            projectType = tProjectRecord.projectType ?: 0,
            approvalStatus = tProjectRecord.approvalStatus ?: 0,
            approvalTime = if (tProjectRecord.approvalTime == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
            },
            approver = tProjectRecord.approver ?: "",
            bgId = tProjectRecord.bgId?.toString(),
            bgName = tProjectRecord.bgName ?: "",
            ccAppId = tProjectRecord.ccAppId ?: 0,
            ccAppName = tProjectRecord.ccAppName ?: "",
            centerId = tProjectRecord.centerId?.toString(),
            centerName = tProjectRecord.centerName ?: "",
            createdAt = DateTimeUtil.toDateTime(tProjectRecord.createdAt, "yyyy-MM-dd"),
            creator = tProjectRecord.creator ?: "",
            dataId = tProjectRecord.dataId ?: 0,
            deployType = tProjectRecord.deployType ?: "",
            deptId = tProjectRecord.deptId?.toString(),
            deptName = tProjectRecord.deptName ?: "",
            description = tProjectRecord.description ?: "",
            extra = tProjectRecord.extra ?: "",
            secrecy = tProjectRecord.isSecrecy,
            helmChartEnabled = tProjectRecord.isHelmChartEnabled,
            kind = tProjectRecord.kind,
            logoAddr = tProjectRecord.logoAddr ?: "",
            remark = tProjectRecord.remark ?: "",
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            useBk = tProjectRecord.useBk,
            enabled = tProjectRecord.enabled ?: true,
            gray = grayProjectSet.contains(tProjectRecord.englishName),
            hybridCcAppId = tProjectRecord.hybridCcAppId,
            enableExternal = tProjectRecord.enableExternal,
            enableIdc = tProjectRecord.enableIdc,
            offlined = tProjectRecord.isOfflined
        )
    }

    private fun convertFile(inputStream: InputStream): File {
        val logo = Files.createTempFile("default_", ".png").toFile()

        logo.outputStream().use {
            inputStream.copyTo(it)
        }

        return logo
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = okHttpClient.newBuilder().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    private fun deleteProjectFromAuth(projectId: String, accessToken: String, retry: Boolean = true) {
        logger.warn("Deleting the project $projectId from auth")
        try {
            val url = "$authUrl/$projectId?access_token=$accessToken"
            val request = Request.Builder().url(url).delete().build()
            val responseContent = request(request, "Fail to delete the project $projectId")
            logger.info("Get the delete project $projectId response $responseContent")
            val response: Response<Any?> = objectMapper.readValue(responseContent)
            if (response.code.toInt() != 0) {
                logger.warn("Fail to delete the project $projectId with response $responseContent")
                deleteProjectFromAuth(projectId, accessToken, false)
            }
            logger.info("Finish deleting the project $projectId from auth")
        } catch (t: Throwable) {
            logger.warn("Fail to delete the project $projectId from auth", t)
            if (retry) {
                deleteProjectFromAuth(projectId, accessToken, false)
            }
        }
    }

    fun validate(
        validateType: ProjectValidateType,
        name: String,
        englishName: String? = null
    ) {
        if (name.isBlank()) {
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_EMPTY))
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.length > 12) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_TOO_LONG))
                }
                if (projectDao.checkProjectNameByEnglishName(dslContext, name, englishName)) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 32 个字符+数字，以小写字母开头
                if (name.length < 2) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_INTERVAL_ERROR))
                }
                if (name.length > 32) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_INTERVAL_ERROR))
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_COMBINATION_ERROR))
                }
                if (projectDao.checkEnglishName(dslContext, name)) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_EXIST))
                }
            }
        }
    }

    fun getProjectIdInAuth(projectCode: String, accessToken: String): String? {
        try {
            val url = "$authUrl/$projectCode?access_token=$accessToken"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url).use { resp ->
                val responseStr = resp.body()!!.string()
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["code"] as Int == 0) {
                    response["project_id"] as String
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Get project info error", e)
            throw RuntimeException("Get project info error: ${e.message}")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        const val PROJECT_LIST = "project_list"
        const val PROJECT_CREATE = "project_create"
        const val PROJECT_UPDATE = "project_update"
    }
}