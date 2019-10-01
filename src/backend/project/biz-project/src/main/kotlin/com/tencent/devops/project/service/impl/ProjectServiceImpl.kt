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

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BkArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.BkAuthServiceCode
import com.tencent.devops.common.auth.code.BkPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_CREATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_CREATE
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_CREATE
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_UPDATE
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.util.ImageUtil
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.regex.Pattern

/**
 * 蓝鲸权限中心管控的项目服务实现
 */
@Service
class ProjectServiceImpl @Autowired constructor(
        private val projectPermissionService: ProjectPermissionService,
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val projectJmxApi: ProjectJmxApi,
        private val redisOperation: RedisOperation,
        private val objectMapper: ObjectMapper,
        private val gray: Gray,
        private val jmxApi: ProjectJmxApi,
        private val rabbitTemplate: RabbitTemplate,
        private val client: Client,
        private val authProjectApi: AuthProjectApi,
        private val pipelineAuthServiceCode: BkPipelineAuthServiceCode

) : ProjectService {

    @Value("\${auth.url}")
    private lateinit var authUrl: String

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw OperationException("名称不能为空")
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.length < 4 || name.length > 12) {
                    throw OperationException("项目名至多4-12个字符")
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw OperationException("项目名已经存在")
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 32 个字符+数字，以小写字母开头
                if (name.length < 2 || name.length > 32) {
                    throw OperationException("英文名长度在3-32个字符")
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw OperationException("英文名是字符+数字组成，并以小写字母开头")
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw OperationException("英文名已经存在")
                }
            }
        }
    }

    /**
     * 创建项目信息
     */
    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo): String {
        validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
        validate(ProjectValidateType.english_name, projectCreateInfo.englishName)

        // 随机生成首字母图片
        val firstChar = projectCreateInfo.englishName.substring(0, 1).toUpperCase()
        val logoFile = ImageUtil.drawImage(
            firstChar,
            Width,
            Height
        )
        try {
            // 保存Logo文件
            val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
            val result =
                CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, logoFile, FileChannelTypeEnum.WEB_SHOW.name)
            if (result.isNotOk()) {
                throw OperationException("${result.status}:${result.message}")
            }
            val logoAddress = result.data!!

            try {
                // 注册项目到权限中心
                projectPermissionService.createResources(
                    userId = userId,
                    projectList = listOf(
                        ResourceRegisterInfo(
                            projectCreateInfo.englishName,
                            projectCreateInfo.projectName
                        )
                    )
                )
            } catch (e: Exception) {
                logger.warn("权限中心创建项目信息： $projectCreateInfo", e)
                throw OperationException("权限中心创建项目失败")
            }

            val projectId = UUIDUtil.generate()
            val userDeptDetail = UserDeptDetail(
                bgName = "",
                bgId = 1,
                centerName = "",
                centerId = 1,
                deptName = "",
                deptId = 1,
                groupId = 0,
                groupName = ""
            )
            try {
                projectDao.create(dslContext, userId, logoAddress, projectCreateInfo, userDeptDetail, projectId)
                return projectId
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectCreateInfo", e)
                throw OperationException("项目名或者英文名重复")
            } catch (ignored: Throwable) {
                logger.warn(
                    "Fail to create the project ($projectCreateInfo)",
                    ignored
                )
                projectPermissionService.deleteResource(projectCode = projectCreateInfo.englishName)

                throw ignored
            }
        } finally {
            if (logoFile.exists()) {
                logoFile.delete()
            }
        }
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return packagingBean(record, grayProjectSet())
    }

    override fun update(userId: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo): Boolean {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            try {
                projectDao.update(dslContext, userId, projectId, projectUpdateInfo)
                projectPermissionService.modifyResource(
                    projectCode = projectUpdateInfo.englishName,
                    projectName = projectUpdateInfo.projectName
                )
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException("项目名或英文名重复")
            }
            success = true
        } finally {
            projectJmxApi.execute(ProjectJmxApi.PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
        return success
    }

    /**
     * 启用/停用项目
     */
    override fun updateUsableStatus(userId: String, projectId: String, enabled: Boolean) {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            logger.info("[$userId|$projectId|$enabled] Start to update project usable status")
            if (authProjectApi.getProjectUsers(pipelineAuthServiceCode, projectId, BkAuthGroup.MANAGER).contains(
                            userId
                    )
            ) {
                val updateCnt = projectDao.updateUsableStatus(dslContext, userId, projectId, enabled)
                if (updateCnt != 1) {
                    logger.warn("更新数据库出错，变更行数为:$updateCnt")
                }
            } else {
                throw OperationException("没有该项目的操作权限")
            }
            logger.info("[$userId|[$projectId] Project usable status is changed to $enabled")
            success = true
        } finally {
            jmxApi.execute(PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
    }

    /**
     * 获取所有项目信息
     */
    override fun list(userId: String): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = projectPermissionService.getUserProjects(userId)

            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(dslContext, projects).map {
                list.add(packagingBean(it, grayProjectSet()))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun list(projectCodes: Set<String>): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()

            val grayProjectSet = grayProjectSet()

            projectDao.listByCodes(dslContext, projectCodes).filter { it.enabled == null || it.enabled }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getAllProject(): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.getAllProject(dslContext).filter { it.enabled == null || it.enabled }.map {
                list.add(recordToBean(it))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    /**
     * 获取用户已的可访问项目列表
     */
    override fun getProjectByUser(userName: String): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectList = projectPermissionService.getUserProjectsAvailable(userName)

            val list = ArrayList<ProjectVO>()
            val projectCodes = projectList.map { it.key }

            val grayProjectSet = grayProjectSet()

            projectDao.listByCodes(dslContext, projectCodes.toSet()).filter { it.enabled == null || it.enabled }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getNameByCode(projectCodes: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        projectDao.listByCodes(dslContext, projectCodes.split(",").toSet()).map {
            map.put(it.englishName, it.projectName)
        }
        return map
    }

    override fun grayProjectSet() =
        (redisOperation.getSetMembers(gray.getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()

    private fun recordToBean(tProjectRecord: TProjectRecord): ProjectVO {
        return ProjectVO(
                id = tProjectRecord.id,
                projectId = tProjectRecord.projectId ?: "",
                projectName = tProjectRecord.projectName,
                projectCode = tProjectRecord.englishName ?: "",
                projectType = tProjectRecord.projectType ?: 0,
                approvalStatus = tProjectRecord.approvalStatus ?: 0,
                approvalTime = if (tProjectRecord.approvalTime == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(tProjectRecord.approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
                },
                approver = tProjectRecord.approver ?: "",
                bgId = tProjectRecord.bgId,
                bgName = tProjectRecord.bgName ?: "",
                ccAppId = tProjectRecord.ccAppId ?: 0,
                ccAppName = tProjectRecord.ccAppName ?: "",
                centerId = tProjectRecord.centerId ?: 0,
                centerName = tProjectRecord.centerName ?: "",
                createdAt = DateTimeUtil.toDateTime(tProjectRecord.createdAt, "yyyy-MM-dd'T'HH:mm:ssZ"),
                creator = tProjectRecord.creator ?: "",
                dataId = tProjectRecord.dataId ?: 0,
                deployType = tProjectRecord.deployType ?: "",
                deptId = tProjectRecord.deptId ?: 0,
                deptName = tProjectRecord.deptName ?: "",
                description = tProjectRecord.description ?: "",
                englishName = tProjectRecord.englishName ?: "",
                extra = tProjectRecord.extra ?: "",
                isOfflined = tProjectRecord.isOfflined,
                isSecrecy = tProjectRecord.isSecrecy,
                isHelmChartEnabled = tProjectRecord.isHelmChartEnabled,
                kind = tProjectRecord.kind,
                logoAddr = tProjectRecord.logoAddr ?: "",
                remark = tProjectRecord.remark ?: "",
                updatedAt = if (tProjectRecord.updatedAt == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd'T'HH:mm:ssZ")
                },
                useBk = tProjectRecord.useBk,
                enabled = tProjectRecord.enabled,
                gray = false,
                hybridCcAppId = tProjectRecord.hybridCcAppId,
                enableExternal = tProjectRecord.enableExternal
        )
    }

    private fun packagingBean(tProjectRecord: TProjectRecord, grayProjectSet: Set<String>): ProjectVO {
        return ProjectVO(
            id = tProjectRecord.id,
            projectId = tProjectRecord.projectId ?: "",
            projectName = tProjectRecord.projectName,
            projectCode = tProjectRecord.englishName ?: "",
            projectType = tProjectRecord.projectType ?: 0,
            approvalStatus = tProjectRecord.approvalStatus ?: 0,
            approvalTime = if (tProjectRecord.approvalTime == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
            },
            approver = tProjectRecord.approver ?: "",
            bgId = tProjectRecord.bgId,
            bgName = tProjectRecord.bgName ?: "",
            ccAppId = tProjectRecord.ccAppId ?: 0,
            ccAppName = tProjectRecord.ccAppName ?: "",
            centerId = tProjectRecord.centerId ?: 0,
            centerName = tProjectRecord.centerName ?: "",
            createdAt = DateTimeUtil.toDateTime(tProjectRecord.createdAt, "yyyy-MM-dd"),
            creator = tProjectRecord.creator ?: "",
            dataId = tProjectRecord.dataId ?: 0,
            deployType = tProjectRecord.deployType ?: "",
            deptId = tProjectRecord.deptId ?: 0,
            deptName = tProjectRecord.deptName ?: "",
            description = tProjectRecord.description ?: "",
            englishName = tProjectRecord.englishName ?: "",
            extra = tProjectRecord.extra ?: "",
            isOfflined = tProjectRecord.isOfflined,
            isSecrecy = tProjectRecord.isSecrecy,
            isHelmChartEnabled = tProjectRecord.isHelmChartEnabled,
            kind = tProjectRecord.kind,
            logoAddr = tProjectRecord.logoAddr ?: "",
            remark = tProjectRecord.remark ?: "",
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            useBk = tProjectRecord.useBk,
            enabled = tProjectRecord.enabled,
            gray = grayProjectSet.contains(tProjectRecord.englishName),
            enableExternal = tProjectRecord.enableExternal
        )
    }

    override fun updateLogo(
        userId: String,
        projectId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        logger.info("Update the logo of project $projectId")
        val project = projectDao.get(dslContext, projectId)
        if (project != null) {
            var logoFile: File? = null
            try {
                logoFile = FileUtil.convertTempFile(inputStream)
                val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
                val result =
                    CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, logoFile, FileChannelTypeEnum.WEB_SHOW.name)
                if (result.isNotOk()) {
                    return Result(result.status, result.message, false)
                }
                projectDao.updateLogoAddress(dslContext, userId, projectId, result.data!!)
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException("更新项目logo失败")
            } finally {
                logoFile?.delete()
            }
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException("查询不到有效的项目")
        }
        return Result(true)
    }

    override fun updateEnabled(
        userId: String,
        accessToken: String,
        projectId: String,
        enabled: Boolean
    ): Result<Boolean> {
        logger.info("Update the enabled of project $projectId")
        val project = projectDao.get(dslContext, projectId)
        if (project != null) {
            projectDao.updateEnabled(dslContext, userId, projectId, enabled)
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException("查询不到有效的项目")
        }
        return Result(true)
    }

    override fun getProjectEnNamesByOrganization(userId: String, bgId: Long?, deptName: String?, centerName: String?, interfaceName: String?): List<String> {
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
        }    }

    override fun getOrCreatePreProject(userId: String, accessToken: String): ProjectVO {
        val projectCode = "_$userId"
        var userProjectRecord = projectDao.getByEnglishName(dslContext, projectCode)
        if (userProjectRecord != null) {
            return packagingBean(userProjectRecord, setOf())
        }

        //TODO: 此处报红为内部版代码定义的对应结构存在，
        val projectCreateInfo = ProjectCreateInfo(
                project_name = projectCode,
                english_name = projectCode,
                project_type = ProjectTypeEnum.SUPPORT_PRODUCT.index,
                description = "prebuild project for $userId",
                bg_id = 0L,
                bg_name = "",
                dept_id = 0L,
                dept_name = "",
                center_id = 0L,
                center_name = "",
                is_secrecy = false,
                kind = 0
        )

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            // 随机生成图片
            val logoFile = drawImage(projectCreateInfo.english_name.substring(0, 1).toUpperCase())
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.english_name)

                var projectId = getProjectIdInAuth(projectCode, accessToken)

                if (null == projectId) {
                    // 创建AUTH项目
                    val authUrl = "$authUrl?access_token=$accessToken"
                    val param: MutableMap<String, String> =
                            mutableMapOf("project_code" to projectCreateInfo.english_name)
                    val mediaType = MediaType.parse("application/json; charset=utf-8")
                    val json = objectMapper.writeValueAsString(param)
                    val requestBody = RequestBody.create(mediaType, json)
                    val request = Request.Builder().url(authUrl).post(requestBody).build()
                    val responseContent = request(request, "调用权限中心创建项目失败")
                    val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
                    if (result.isNotOk()) {
                        logger.warn("Fail to create the project of response $responseContent")
                        throw OperationException("调用权限中心创建项目失败: ${result.message}")
                    }
                    val authProjectForCreateResult = result.data
                    projectId = if (authProjectForCreateResult != null) {
                        if (authProjectForCreateResult.project_id.isBlank()) {
                            throw OperationException("权限中心创建的项目ID无效")
                        }
                        authProjectForCreateResult.project_id
                    } else {
                        logger.warn("Fail to get the project id from response $responseContent")
                        throw OperationException("权限中心创建的项目ID无效")
                    }
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
                    throw OperationException("项目名或者英文名重复")
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

    override fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
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

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        logger.info("getProjectUsers accessToken is :$accessToken,userId is :$userId,projectCode is :$projectCode")
        // 检查用户是否有查询项目下用户列表的权限
        val validateResult = verifyUserProjectPermission(accessToken, projectCode, userId)
        logger.info("getProjectUsers validateResult is :$validateResult")
        val validateFlag = validateResult.data
        if (null == validateFlag || !validateFlag) {
            val messageResult = MessageCodeUtil.generateResponseDataObject<String>(CommonMessageCode.PERMISSION_DENIED)
            return Result(messageResult.status, messageResult.message, null)
        }
        val projectUserList = authProjectApi.getProjectUsers(pipelineAuthServiceCode, projectCode)
        logger.info("getProjectUsers projectUserList is :$projectUserList")
        return Result(projectUserList)
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String, serviceCode: AuthServiceCode): List<UserRole> {
        val groupAndUsersList = authProjectApi.getProjectGroupAndUserList(serviceCode, projectCode)
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

    companion object {
        private const val Width = 128
        private const val Height = 128
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)!!
        private const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9]+"
    }
}
