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

package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectSourceEnum
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.service.impl.TxProjectServiceImpl
import com.tencent.devops.project.util.ProjectUtils
import com.tencent.devops.stream.api.service.ServiceGitForAppResource
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList", "TooManyFunctions", "LongMethod", "MagicNumber", "TooGenericExceptionCaught")
class ProjectLocalService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val authProjectApi: AuthProjectApi,
    bkAuthProperties: BkAuthProperties,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val gray: Gray,
    private val jmxApi: ProjectJmxApi,
    private val projectService: ProjectService,
    private val projectTagService: ProjectTagService,
    private val client: Client,
    private val projectPermissionService: ProjectPermissionService,
    private val txProjectServiceImpl: TxProjectServiceImpl,
    private val projectExtPermissionService: ProjectExtPermissionService,
    private val bkTag: BkTag
) {
    private var authUrl: String = "${bkAuthProperties.url}/projects"

    @Value("\${tag.stream:#{null}}")
    private val streamTag: String? = null

    fun listForApp(
        userId: String,
        page: Int,
        pageSize: Int,
        searchName: String?
    ): Pagination<AppProjectVO> {

        val finalRecords = mutableListOf<AppProjectVO>()

        // 先查询GITCI的项目
        if (page == 1) {
            val gitCIProjectList = bkTag.invokeByTag(streamTag) {
                try {
                    client.get(ServiceGitForAppResource::class).getGitCIProjectList(userId, 1, 100, searchName)
                } catch (e: Exception) {
                    logger.warn("ServiceGitForAppResource is error", e)
                    return@invokeByTag null
                }
            }
            gitCIProjectList?.data?.records?.let {
                finalRecords.addAll(it)
            }
        }

        // 再查询蓝盾项目
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit
        val projectIds = txProjectServiceImpl.getProjectFromAuth(userId, null)
        // 如果使用搜索 且 总数量少于1000 , 则全量获取
        if (searchName != null &&
            searchName.isNotEmpty() &&
            projectDao.countByEnglishName(dslContext, projectIds) < 1000
        ) {
            val records = projectDao.listByEnglishName(
                dslContext = dslContext,
                englishNameList = projectIds,
                enabled = true
            ).asSequence().filter {
                it.projectName.contains(searchName, true)
            }.map {
                AppProjectVO(
                    projectCode = it.englishName,
                    projectName = it.projectName,
                    logoUrl = if (it.logoAddr.startsWith("http://radosgw.open.oa.com")) {
                        "https://dev-download.bkdevops.qq.com/images" +
                                it.logoAddr.removePrefix("http://radosgw.open.oa.com")
                    } else {
                        it.logoAddr
                    },
                    projectSource = ProjectSourceEnum.BK_CI.id
                )
            }.toList()

            finalRecords.addAll(records)

            return Pagination(false, finalRecords)
        } else {
            val records = projectDao.listByEnglishName(
                dslContext = dslContext,
                englishNameList = projectIds,
                offset = offset,
                limit = limit,
                searchName = searchName,
                enabled = true
            ).map {
                AppProjectVO(
                    projectCode = it.englishName,
                    projectName = it.projectName,
                    logoUrl = if (it.logoAddr.startsWith("http://radosgw.open.oa.com")) {
                        "https://dev-download.bkdevops.qq.com/images" +
                                it.logoAddr.removePrefix("http://radosgw.open.oa.com")
                    } else {
                        it.logoAddr
                    },
                    projectSource = ProjectSourceEnum.BK_CI.id
                )
            }

            val hasNext = if (records.size < limit) {
                false
            } else {
                val countByEnglishName = projectDao.countByEnglishName(dslContext, projectIds, searchName)
                countByEnglishName > offset + limit
            }

            finalRecords.addAll(records)

            return Pagination(hasNext, finalRecords)
        }
    }

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
            ).filter { it.enabled == null || it.enabled }.map { it.englishName }.toList()
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
        var projectName = projectCode
        val tmpProjectRecord = projectDao.getByCnName(dslContext, projectName)
        if (tmpProjectRecord != null) {
            projectName = "_$userId" + System.currentTimeMillis()
        }

        val projectCreateInfo = ProjectCreateInfo(
            projectName = projectName,
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

        val projectId = getProjectIdInAuth(projectCode, accessToken)

        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val createExt = ProjectCreateExtInfo(
                needValidate = false,
                needAuth = projectId.isNullOrEmpty()
            )
            projectService.create(
                userId = userId,
                projectCreateInfo = projectCreateInfo,
                accessToken = accessToken,
                createExt = createExt,
                projectId = projectId,
                channel = ProjectChannelCode.PREBUILD
            )
        } catch (e: Exception) {
            logger.warn("Fail to create the project ($projectCreateInfo)", e)
            throw e
        } finally {
            jmxApi.execute(PROJECT_CREATE, System.currentTimeMillis() - startEpoch, success)
        }
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

    @SuppressWarnings("ALL")
    fun getByName(
        name: String,
        nameType: ProjectValidateType,
        organizationId: Long,
        organizationType: String,
        showSecrecy: Boolean? = false
    ): ProjectVO? {
        logger.info("getProjectByName: $name| $nameType| $organizationId| $organizationType| $showSecrecy")
        val projectInfo = when (nameType) {
            ProjectValidateType.english_name -> projectDao.getByEnglishName(dslContext, name)
            ProjectValidateType.project_name -> projectDao.getByCnName(dslContext, name)
        } ?: return null

        if (!showSecrecy!! && projectInfo.isSecrecy) {
            return null
        }

        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> {
                if (projectInfo.bgId == null || projectInfo.bgId != organizationId) {
                    return null
                }
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> {
                if (projectInfo.deptId == null || projectInfo.deptId != organizationId) {
                    return null
                }
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> {
                if (projectInfo.centerId == null || projectInfo.centerId != organizationId) {
                    return null
                }
            }
        }
        return ProjectUtils.packagingBean(projectInfo, grayProjectSet())
    }

    fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        logger.info("getProjectUsers accessToken is :$accessToken,userId is :$userId,projectCode is :$projectCode")
        // 检查用户是否有查询项目下用户列表的权限
//        val validateResult = verifyUserProjectPermission(accessToken, projectCode, userId)
        val validateFlag = projectPermissionService.verifyUserProjectPermission(accessToken, projectCode, userId)
        logger.info("getProjectUsers validateResult is :$validateFlag")
        if (null == validateFlag || !validateFlag) {
            val messageResult = MessageCodeUtil.generateResponseDataObject<String>(CommonMessageCode.PERMISSION_DENIED)
            return Result(messageResult.status, messageResult.message, null)
        }
        val projectUserList = authProjectApi.getProjectUsers(bsPipelineAuthServiceCode, projectCode)
        logger.info("getProjectUsers projectUserList is :$projectUserList")
        return Result(projectUserList)
    }

    fun getProjectUserRoles(
        accessToken: String,
        userId: String,
        projectCode: String,
        serviceCode: AuthServiceCode
    ): List<UserRole> {
        val groupAndUsersList = authProjectApi.getProjectGroupAndUserList(serviceCode, projectCode)
        return groupAndUsersList.filter { it.userIdList.contains(userId) }
            .map {
                // 因历史原因,前端是通过roleName==manager 来判断是否为管理员,故此处需兼容
                if (it.displayName == DefaultGroupType.MANAGER.displayName) {
                    UserRole(it.displayName, it.roleId, DefaultGroupType.MANAGER.value, it.type)
                } else {
                    UserRole(it.displayName, it.roleId, it.roleName, it.type)
                }
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

    private fun grayProjectSet() = gray.grayProjectSet(redisOperation)

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = okHttpClient.newBuilder().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , " +
                                "message ${response.message()} and response $responseContent")
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

    fun createGitCIProject(userId: String, gitProjectId: Long, gitProjectName: String?): ProjectVO {
        val projectCode = "git_$gitProjectId"
        var gitCiProject = projectDao.getByEnglishName(dslContext, projectCode)
        if (gitCiProject != null) {
            return ProjectUtils.packagingBean(gitCiProject, setOf())
        }

        val projectCreateInfo = ProjectCreateInfo(
            projectName = gitProjectName ?: projectCode,
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

        try {
            val createExt = ProjectCreateExtInfo(
                needValidate = false,
                needAuth = false
            )
            projectService.create(
                userId = userId,
                projectCreateInfo = projectCreateInfo,
                accessToken = null,
                createExt = createExt,
                projectId = projectCode,
                channel = ProjectChannelCode.GITCI
            )

            // stream项目自动把流量指向gitCI集群, 注意此tag写死在代码内,若对应集群的consulTag调整需要变更代码
            projectTagService.updateTagByProject(projectCreateInfo.englishName, streamTag)
        } catch (e: Throwable) {
            logger.error("Create project failed,", e)
            throw e
        }
        gitCiProject = projectDao.getByEnglishName(dslContext, projectCode)
        return ProjectUtils.packagingBean(gitCiProject!!, setOf())
    }

    fun createUser2ProjectByApp(
        userId: String,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        logger.info("[createUser2ProjectByApp] |$userId|$projectCode｜$roleId $roleName")
        projectDao.get(dslContext, projectCode) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST)
        )
        return projectExtPermissionService.createUser2Project(
            createUser = "", // 应用态无需校验创建者身份
            projectCode = projectCode,
            roleId = roleId,
            roleName = roleName,
            userIds = arrayListOf(userId),
            checkManager = false
        )
    }

    fun getProjectRole(
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        logger.info("[getProjectRole] $projectId")
        val queryProject = projectDao.get(dslContext, projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST)
        )
        return authProjectApi.getProjectRoles(
            bsPipelineAuthServiceCode,
            queryProject.englishName,
            queryProject.projectId
        ).toMutableList()
    }

    fun createPipelinePermissionByApp(
        userId: String,
        projectId: String,
        permission: String,
        resourceType: String,
        resourceTypeCode: String
    ): Boolean {
        logger.info("createPipelinePermissionByApp $userId $projectId $permission $resourceType")
        projectDao.get(dslContext, projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))

        val createUserList = userId.split(",")
        return grantInstancePermission(
            userId = userId,
            projectId = projectId,
            permission = permission,
            resourceType = resourceType,
            resourceCode = resourceTypeCode,
            createUserList = createUserList,
            checkManager = false
        )
    }

    fun updateRelationId(projectCode: String, relationId: String) {
        projectDao.updateRelationByCode(dslContext, projectCode, relationId)
    }

    fun grantInstancePermission(
        userId: String,
        projectId: String,
        permission: String,
        resourceType: String,
        resourceCode: String,
        createUserList: List<String>,
        checkManager: Boolean? = true
    ): Boolean {
        logger.info("createpipeline|$userId|$projectId|$permission|$resourceType|$resourceCode")
        if (checkManager!!) {
            // 操作人必须为项目的管理员
            if (!authProjectApi.checkProjectManager(userId, bsPipelineAuthServiceCode, projectId)) {
                logger.error("$userId is not manager for project[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.NOT_MANAGER,
                    params = arrayOf(userId, projectId))))
            }
        }

        // 必须用户在项目下才能授权
        createUserList.forEach {
            if (!projectPermissionService.verifyUserProjectPermission(
                    accessToken = null,
                    projectCode = projectId,
                    userId = userId)) {
                logger.error("createPipelinePermission userId is not project user,userId[$it] projectId[$projectId]")
                throw OperationException((MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.USER_NOT_PROJECT_USER,
                    params = arrayOf(userId, projectId))))
            }
        }

        return projectExtPermissionService.grantInstancePermission(
            userId = userId,
            projectId = projectId,
            action = permission,
            resourceType = resourceType,
            resourceCode = resourceCode,
            userList = createUserList
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectLocalService::class.java)
        const val PROJECT_LIST = "project_list"
        const val PROJECT_CREATE = "project_create"
        const val PROJECT_UPDATE = "project_update"
    }
}
