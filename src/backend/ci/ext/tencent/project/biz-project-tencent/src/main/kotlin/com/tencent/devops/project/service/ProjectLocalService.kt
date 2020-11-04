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
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_CREATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_CREATE
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.PaasCCCreateProject
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.service.job.SynProjectService.Companion.ENGLISH_NAME_PATTERN
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ImageUtil.drawImage
import com.tencent.devops.project.util.ProjectUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.ArrayList
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException
import org.springframework.dao.DuplicateKeyException as DuplicateKeyException1

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
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bkAuthProperties: BkAuthProperties,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val projectPermissionService: ProjectPermissionService,
    private val gray: Gray,
    private val repoGray: RepoGray,
    private val jmxApi: ProjectJmxApi,
    private val bkRepoClient: BkRepoClient,
    private val projectService: ProjectService
) {
    private var authUrl: String = "${bkAuthProperties.url}/projects"

    fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long?,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
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

    fun getProjectEnNamesByCenterId(
        userId: String,
        centerId: Long?,
        interfaceName: String? = "ProjectLocalService"
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByGroupId(
                dslContext = dslContext,
                bgId = null,
                deptId = null,
                centerId = centerId
            )?.filter { it.enabled == null || it.enabled }?.map { it.englishName }?.toList() ?: emptyList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    fun getProjectEnNamesByOrganization(
        userId: String,
        deptId: Long?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByOrganization(
                dslContext = dslContext,
                deptId = deptId,
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
            return ProjectUtils.packagingBean(userProjectRecord, setOf())
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

            projectService.create(
                    userId = userId,
                    projectCreateInfo = projectCreateInfo,
                    accessToken = accessToken,
                    isUserProject = true
            )
        } catch (e: Exception) {
            logger.warn("Fail to create the project ($projectCreateInfo)", e)
            throw e
        }
        finally {
            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }
//            // 随机生成图片
//            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())

//            var projectId = getProjectIdInAuth(projectCode, accessToken)
//            try {


//                // 发送服务器
//                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)
//
//                if (null == projectId) {
//                    // 创建AUTH项目
//                    val authUrl = "$authUrl?access_token=$accessToken"
//                    val param: MutableMap<String, String> =
//                        mutableMapOf("project_code" to projectCreateInfo.englishName)
//                    val mediaType = MediaType.parse("application/json; charset=utf-8")
//                    val json = objectMapper.writeValueAsString(param)
//                    val requestBody = RequestBody.create(mediaType, json)
//                    val request = Request.Builder().url(authUrl).post(requestBody).build()
//                    val responseContent =
//                        request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.CALL_PEM_FAIL))
//                    val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
//                    if (result.isNotOk()) {
//                        logger.warn("Fail to create the project of response $responseContent")
//                        throw OperationException(
//                            MessageCodeUtil.generateResponseDataObject<String>(
//                                ProjectMessageCode.CALL_PEM_FAIL_PARM, arrayOf(result.message!!)
//                            ).message!!
//                        )
//                    }
//                    val authProjectForCreateResult = result.data
//                    projectId = if (authProjectForCreateResult != null) {
//                        if (authProjectForCreateResult.project_id.isBlank()) {
//                            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_ID_INVALID))
//                        }
//                        authProjectForCreateResult.project_id
//                    } else {
//                        logger.warn("Fail to get the project id from response $responseContent")
//                        throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_ID_INVALID))
//                    }
//                }
//                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户机构信息                try {
//
//                val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
//                logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")
//                if (createSuccess) {
//                    repoGray.addGrayProject(projectCreateInfo.englishName, redisOperation)
//                    logger.info("add project ${projectCreateInfo.englishName} to repoGrey")
//                }
//
//                try {
//                    projectDao.create(
//                        dslContext = dslContext,
//                        userId = userId,
//                        logoAddress = logoAddress,
//                        projectCreateInfo = projectCreateInfo,
//                        userDeptDetail = userDeptDetail,
//                        projectId = projectId,
//                        channelCode = ProjectChannelCode.BS
//                    )
//                } catch (e: DuplicateKeyException1) {
//                    logger.warn("Duplicate project $projectCreateInfo", e)
//                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
//                } catch (t: Throwable) {
//                    logger.warn("Fail to create the project ($projectCreateInfo)", t)
//                    deleteProjectFromAuth(projectId, accessToken)
//                    throw t
//                }
//
//                rabbitTemplate.convertAndSend(
//                    EXCHANGE_PAASCC_PROJECT_CREATE,
//                    ROUTE_PAASCC_PROJECT_CREATE,
//                    PaasCCCreateProject(
//                        userId = userId,
//                        accessToken = accessToken,
//                        projectId = projectId,
//                        retryCount = 0,
//                        projectCreateInfo = projectCreateInfo
//                    )
//                )
//                success = true
//            } finally {
//                if (logoFile.exists()) {
//                    logoFile.delete()
//                }
//            }
//        finally {
//            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
//        }

        userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        return ProjectUtils.packagingBean(userProjectRecord!!, setOf())
    }

    fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val grayProjectSet = grayProjectSet()
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroup(dslContext, bgName, deptName, centerName).filter { it.enabled == null || it.enabled }
                .map {
                    list.add(ProjectUtils.packagingBean(it, grayProjectSet))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun getProjectByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
    ): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val grayProjectSet = grayProjectSet()
            val list = ArrayList<ProjectVO>()
            val records = when (organizationType) {
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> {
                    projectDao.listByOrganization(dslContext, organizationId, deptName, centerName)
                }
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> {
                    projectDao.listByOrganization(dslContext, organizationId, centerName)
                }
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> {
                    projectDao.listByGroupId(dslContext, null, null, organizationId)
                }
                else -> {
                    null
                }
            }
            records?.filter { it.enabled == null || it.enabled }
                ?.map {
                    list.add(ProjectUtils.packagingBean(it, grayProjectSet))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun getProjectByGroupId(userId: String, bgId: Long?, deptId: Long?, centerId: Long?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val grayProjectSet = grayProjectSet()
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroupId(dslContext, bgId, deptId, centerId).filter { it.enabled == null || it.enabled }
                .map {
                    list.add(ProjectUtils.packagingBean(it, grayProjectSet))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record, grayProjectSet())
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

    private fun grayProjectSet() = gray.grayProjectSet(redisOperation)

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

    fun getProjectIdInAuth(projectCode: String, accessToken: String): String? {
        try {
            val url = "$authUrl/$projectCode?access_token=$accessToken"
            logger.info("Get request url: $url")
            OkhttpUtils.doGet(url).use { resp ->
                val responseStr = resp.body()!!.string()
                logger.info("responseBody: $responseStr")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
                return if (response["code"] as Int == 0) {
                    val responseData = response["data"] as Map<String, Any>
                    return responseData["project_id"] as String
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Get project info error", e)
            throw RuntimeException("Get project info error: ${e.message}")
        }
    }

    fun createGitCIProject(userId: String, gitProjectId: Long): ProjectVO {
        val projectCode = "git_$gitProjectId"
        var gitCiProject = projectDao.getByEnglishName(dslContext, projectCode)
        if (gitCiProject != null) {
            return ProjectUtils.packagingBean(gitCiProject, setOf())
        }

        val projectCreateInfo = ProjectCreateInfo(
            projectName = projectCode,
            englishName = projectCode,
            projectType = ProjectTypeEnum.SUPPORT_PRODUCT.index,
            description = "git ci project for git projectId: $gitProjectId",
            bgId = 0L,
            bgName = "",
            deptId = 0L,
            deptName = "",
            centerId = 0L,
            centerName = "",
            secrecy = false,
            kind = 0
        )

        try{
            projectService.create(
                    userId = userId,
                    projectCreateInfo = projectCreateInfo,
                    accessToken = null,
                    isUserProject = false
            )
        } catch (e: Throwable) {
            logger.error("Create project failed,", e)
            throw e
        }

//        try {
//            // 随机生成图片
//            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
//            try {
//                // 发送服务器
//                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)
//                val userDeptDetail = tofService.getUserDeptDetail(userId, "") // 获取用户组织架构信息
//
//                val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
//                logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")
//                if (createSuccess) {
//                    repoGray.addGrayProject(projectCreateInfo.englishName, redisOperation)
//                    logger.info("add project ${projectCreateInfo.englishName} to repoGrey")
//                }
//
//                logger.info("add project ${projectCreateInfo.englishName} to repoGrey")
//                projectDao.create(
//                    dslContext = dslContext,
//                    userId = userId,
//                    logoAddress = logoAddress,
//                    projectCreateInfo = projectCreateInfo,
//                    userDeptDetail = userDeptDetail,
//                    projectId = projectCode,
//                    channelCode = ProjectChannelCode.BS
//                )
//            } finally {
//                if (logoFile.exists()) {
//                    logoFile.delete()
//                }
//            }
//        } catch (e: Throwable) {
//            logger.error("Create project failed,", e)
//            throw e
//        }

        gitCiProject = projectDao.getByEnglishName(dslContext, projectCode)
        return ProjectUtils.packagingBean(gitCiProject!!, setOf())
    }

    fun createUser2ProjectByUser(
        createUser: String,
        userId: String,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        logger.info("[createUser2ProjectByUser] createUser[$createUser] userId[$userId] projectCode[$projectCode]")

        if (!bkAuthProjectApi.isProjectUser(createUser, bsPipelineAuthServiceCode, projectCode, BkAuthGroup.MANAGER)) {
            logger.error("$createUser is not manager for project[$projectCode]")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NOT_MANAGER))
        }
        return createUser2Project(userId, projectCode, roleId, roleName)
    }

    fun createUser2ProjectByApp(
        organizationType: String,
        organizationId: Long,
        userId: String,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        logger.info("[createUser2ProjectByApp] organizationType[$organizationType], organizationId[$organizationId] userId[$userId] projectCode[$projectCode]")
        var bgId: Long? = null
        var deptId: Long? = null
        var centerId: Long? = null
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> bgId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> deptId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> centerId = organizationId
            else -> {
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_TYPE_ERROR)))
            }
        }
        val projectList = getProjectByGroupId(userId, bgId, deptId, centerId)
        if (projectList.isEmpty()) {
            logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectCode] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }

        var isCreate = false
        projectList.forEach { project ->
            if (project.projectCode.equals(projectCode)) {
                isCreate = true
                return@forEach
            }
        }
        if (isCreate) {
            return createUser2Project(userId, projectCode, roleId, roleName)
        } else {
            logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectCode] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
    }

    fun createPipelinePermission(
        createUser: String,
        projectId: String,
        userId: String,
        permission: String,
        resourceType: String,
        resourceTypeCode: String
    ): Boolean {
        logger.info("createPipelinePermission createUser[$createUser] projectId[$projectId] userId[$userId] permissionList[$permission]")
        if (!bkAuthProjectApi.isProjectUser(createUser, bsPipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER)) {
            logger.info("createPipelinePermission createUser is not project manager,createUser[$createUser] projectId[$projectId]")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NOT_MANAGER)))
        }
        val createUserList = userId.split(",")

        createUserList?.forEach {
            if (!bkAuthProjectApi.isProjectUser(it, bsPipelineAuthServiceCode, projectId, null)) {
                logger.info("createPipelinePermission userId is not project manager,userId[$userId] projectId[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
            }
        }

        return createPermission(
            userId = userId,
            projectId = projectId,
            permission = permission,
            resourceType = resourceType,
            authServiceCode = bsPipelineAuthServiceCode,
            resourceTypeCode = resourceTypeCode,
            userList = createUserList
        )
    }

    fun createPipelinePermissionByApp(
        organizationType: String,
        organizationId: Long,
        userId: String,
        projectId: String,
        permission: String,
        resourceType: String,
        resourceTypeCode: String
    ): Boolean {
        logger.info("[createPipelinePermissionByApp] organizationType[$organizationType], organizationId[$organizationId] userId[$userId] projectCode[$projectId], permission[$permission], resourceType[$resourceType],resourceTypeCode[$resourceTypeCode]")
        val projectList = getProjectListByOrg(userId, organizationType, organizationId)
        if (projectList.isEmpty()) {
            logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        var isCreate = false
        projectList.forEach { project ->
            if (project.projectCode == projectId) {
                isCreate = true
                return@forEach
            }
        }
        if (!isCreate) {
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
        }
        val createUserList = userId.split(",")

        createUserList?.forEach {
            if (!bkAuthProjectApi.isProjectUser(it, bsPipelineAuthServiceCode, projectId, null)) {
                logger.error("createPipelinePermission userId is not project user,userId[$it] projectId[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_PROJECT_USER)))
            }
        }

        // TODO:此处bsPipelineAuthServiceCode 也需写成配置化
        return createPermission(
            userId = userId,
            projectId = projectId,
            permission = permission,
            resourceType = resourceType,
            authServiceCode = bsPipelineAuthServiceCode,
            resourceTypeCode = resourceTypeCode,
            userList = createUserList
        )
    }

    fun getProjectRole(
        organizationType: String,
        organizationId: Long,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        logger.info("[getProjectRole] organizationType[$organizationType], organizationId[$organizationId] projectCode[$projectId]")
        val projectList = getProjectListByOrg("", organizationType, organizationId)
        if (projectList.isEmpty()) {
            logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        if (projectList.isEmpty()) {
            logger.error("organizationType[$organizationType] :organizationId[$organizationId]  not project[$projectId] permission ")
            throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_NOT_PROJECT)))
        }
        var queryProject: ProjectVO? = null
        projectList.forEach { project ->
            if (project.projectCode == projectId) {
                queryProject = project
                return@forEach
            }
        }
        var roles = mutableListOf<BKAuthProjectRolesResources>()
        if (queryProject != null) {
            roles = bkAuthProjectApi.getProjectRoles(
                bsPipelineAuthServiceCode,
                queryProject!!.englishName,
                queryProject!!.projectId
            ).toMutableList()
        }
        return roles
    }

    private fun createPermission(
        userId: String,
        userList: List<String>?,
        projectId: String,
        permission: String,
        resourceType: String,
        authServiceCode: AuthServiceCode,
        resourceTypeCode: String
    ): Boolean {
        projectDao.getByEnglishName(dslContext, projectId)
            ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))

        val authPermission = AuthPermission.get(permission)
        val authResourceType = AuthResourceType.get(resourceType)

        return bkAuthPermissionApi.addResourcePermissionForUsers(
            userId = userId,
            projectCode = projectId,
            permission = authPermission,
            serviceCode = authServiceCode,
            resourceType = authResourceType,
            resourceCode = resourceTypeCode,
            userIdList = userList ?: emptyList(),
            supplier = null
        )
    }

    private fun createUser2Project(userId: String, projectId: String, roleId: Int?, roleName: String?): Boolean {
        logger.info("[createUser2Project]  userId[$userId] projectCode[$projectId], roleId[$roleId], roleName[$roleName]")
        val projectInfo = projectDao.getByEnglishName(dslContext, projectId) ?: throw RuntimeException()
        val roleList = bkAuthProjectApi.getProjectRoles(bsPipelineAuthServiceCode, projectId, projectInfo.englishName)
        var authRoleId: String? = null
        roleList.forEach {
            if (roleId == null && roleName.isNullOrEmpty()) {
                if (it.roleName == BkAuthGroup.DEVELOPER.value) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleId != null) {
                if (it.roleId == roleId) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleName != null) {
                if (it.roleName == roleName) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
        }
        return bkAuthProjectApi.createProjectUser(
            userId,
            bsPipelineAuthServiceCode,
            projectInfo.projectId,
            authRoleId!!
        )
    }

    private fun getProjectListByOrg(userId: String, organizationType: String, organizationId: Long): List<ProjectVO> {
        var bgId: Long? = null
        var deptId: Long? = null
        var centerId: Long? = null
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> bgId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> deptId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> centerId = organizationId
            else -> {
                throw OperationException((MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.ORG_TYPE_ERROR)))
            }
        }
        return getProjectByGroupId(userId, bgId, deptId, centerId)
    }

    private fun synAuthProject(
        userId: String,
        accessToken: String,
        englishName: String,
        projectUpdateInfo: ProjectUpdateInfo
    ): Boolean {
        logger.info("synAuthProject by update, $projectUpdateInfo")
        val projectInfo = bkAuthProjectApi.getProjectInfo(bsPipelineAuthServiceCode, englishName)
        if (projectInfo == null) {
            projectPermissionService.createResources(
                userId = userId,
                accessToken = accessToken,
                resourceRegisterInfo = ResourceRegisterInfo(
                    projectUpdateInfo.englishName,
                    projectUpdateInfo.projectName
                ),
                userDeptDetail = null
            )
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectLocalService::class.java)
        const val PROJECT_LIST = "project_list"
        const val PROJECT_CREATE = "project_create"
        const val PROJECT_UPDATE = "project_update"
    }
}