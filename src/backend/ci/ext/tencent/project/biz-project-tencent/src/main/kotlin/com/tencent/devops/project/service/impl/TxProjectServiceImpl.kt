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

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.ApplicationInfo
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectTagService
import com.tencent.devops.project.service.ShardingRoutingRuleAssignService
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ProjectUtils
import com.tencent.devops.support.api.service.ServiceFileResource
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Suppress("ALL")
@Service
class TxProjectServiceImpl @Autowired constructor(
    projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val tofService: TOFService,
    private val bkRepoClient: BkRepoClient,
    private val projectPaasCCService: ProjectPaasCCService,
    private val bsAuthProjectApi: AuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    projectJmxApi: ProjectJmxApi,
    redisOperation: RedisOperation,
    client: Client,
    private val projectDispatcher: ProjectDispatcher,
    authPermissionApi: AuthPermissionApi,
    projectAuthServiceCode: ProjectAuthServiceCode,
    shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService,
    private val managerService: ManagerService,
    private val tokenService: ClientTokenService,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val projectExtPermissionService: ProjectExtPermissionService,
    private val projectTagService: ProjectTagService,
    private val bkTag: BkTag,
    objectMapper: ObjectMapper
) : AbsProjectServiceImpl(
    projectPermissionService = projectPermissionService,
    dslContext = dslContext,
    projectDao = projectDao,
    projectJmxApi = projectJmxApi,
    redisOperation = redisOperation,
    client = client,
    projectDispatcher = projectDispatcher,
    authPermissionApi = authPermissionApi,
    projectAuthServiceCode = projectAuthServiceCode,
    shardingRoutingRuleAssignService = shardingRoutingRuleAssignService,
    objectMapper = objectMapper
) {

    @Value("\${iam.v0.url:#{null}}")
    private var v0IamUrl: String = ""

    @Value("\${tag.v3:#{null}}")
    private var v3Tag: String = ""

    @Value("\${tag.rbac:#{null}}")
    private var rbacTag: String = ""

    @Value("\${tag.auto:#{null}}")
    private val autoTag: String? = null

    @Value("\${tag.stream:#{null}}")
    private val streamTag: String = "stream"

    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    override fun getByEnglishName(userId: String, englishName: String, accessToken: String?): ProjectVO? {
        val projectVO = getInfoByEnglishName(englishName)
        if (projectVO == null) {
            logger.warn("The projectCode $englishName is not exist")
            return null
        }
        // 判断用户是否为管理员，若为管理员则不调用iam
        val isManager = managerService.isManagerPermission(
            userId = userId,
            projectId = englishName,
            resourceType = AuthResourceType.PROJECT,
            authPermission = AuthPermission.VIEW
        )

        if (isManager) {
            logger.info("getByEnglishName $userId is $englishName manager")
            return projectVO
        }
        // 若该项目是该用户创建，并且还未创建成功，并不需要鉴权，直接返回项目详情
        val isNotCreateSuccess = projectVO.creator == userId
            && (projectVO.approvalStatus == ApproveStatus.CREATE_PENDING.status
            || projectVO.approvalStatus == ApproveStatus.CREATE_REJECT.status)
        if (isNotCreateSuccess)
            return projectVO
        val englishNames = getProjectFromAuth(userId, accessToken)
        if (englishNames.isEmpty()) {
            return null
        }
        if (!englishNames.contains(projectVO.englishName)) {
            logger.warn("The user don't have the permission to get the project $englishName")
            return null
        }
        return projectVO
    }

    override fun list(
        userId: String,
        accessToken: String?,
        enabled: Boolean?,
        unApproved: Boolean
    ): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        try {
            val englishNames = getProjectFromAuth(userId, accessToken).toSet()
            if (englishNames.isEmpty() && !unApproved) {
                return emptyList()
            }
            val list = ArrayList<ProjectVO>()
            if (englishNames.isNotEmpty()) {
                projectDao.listByCodes(dslContext, englishNames, enabled = enabled)
                    .map {
                        list.add(ProjectUtils.packagingBean(it))
                    }
            }
            // 将用户创建的项目，但还未审核通过的，一并拉出来，用户项目管理界面
            if (unApproved) {
                projectDao.listUnapprovedByUserId(
                    dslContext = dslContext,
                    userId = userId
                )?.map { list.add(ProjectUtils.packagingBean(it)) }
            }
            return list
        } finally {
            logger.info("$userId|It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getDeptInfo(userId: String): UserDeptDetail {
        try {
            return tofService.getUserDeptDetail(userId, "")
        } catch (e: OperationException) {
            // stream场景下会传公共账号,tof不存在公共账号
            logger.warn("getDeptInfo: $e")
            return UserDeptDetail(
                bgId = "0",
                bgName = "",
                centerId = "0",
                centerName = "",
                deptId = "0",
                deptName = "",
                groupId = "0",
                groupName = ""
            )
        }
    }

    override fun createExtProjectInfo(
        userId: String,
        projectId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo,
        createExtInfo: ProjectCreateExtInfo
    ) {
        // 添加repo项目
        val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
        logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")

        if (createExtInfo.needAuth!!) {
            val newAccessToken = if (accessToken.isNullOrBlank()) {
                bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
            } else accessToken
            // 添加paas项目
            projectPaasCCService.createPaasCCProject(
                userId = userId,
                projectId = projectId,
                accessToken = newAccessToken,
                projectCreateInfo = projectCreateInfo
            )
        }
        // 工蜂CI项目不会添加paas项目，但也需要广播
        projectDispatcher.dispatch(
            ProjectCreateBroadCastEvent(
                userId = userId,
                projectId = projectId,
                projectInfo = projectCreateInfo
            )
        )
    }

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val serviceUrl =
            "$serviceUrlPrefix/service/file/upload?userId=$userId"
        OkhttpUtils.uploadFile(serviceUrl, logoFile).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("$userId upload file:${logoFile.name} fail,responseContent:$responseContent")
                throw ErrorCodeException(errorCode = ProjectMessageCode.SAVE_LOGO_FAIL)
            }
            val result = JsonUtil.to(
                json = responseContent,
                typeReference = object : TypeReference<com.tencent.devops.common.api.pojo.Result<String?>>() {}
            )
            val logoAddress = result.data
            if (logoAddress.isNullOrBlank()) {
                logger.warn("$userId upload file:${logoFile.name} fail,result:$result")
                throw ErrorCodeException(errorCode = ProjectMessageCode.SAVE_LOGO_FAIL)
            }
            return logoAddress
        }
    }

    override fun deleteAuth(projectId: String, accessToken: String?) {
//        logger.warn("Deleting the project $projectId from auth")
//        try {
//            val url = "$authUrl/$projectId?access_token=$accessToken"
//            val request = Request.Builder().url(url).delete().build()
//            val responseContent = request(request, "Fail to delete the project $projectId")
//            logger.info("Get the delete project $projectId response $responseContent")
//            val response: Response<Any?> = objectMapper.readValue(responseContent)
//            if (response.code.toInt() != 0) {
//                logger.warn("Fail to delete the project $projectId with response $responseContent")
//            }
//            logger.info("Finish deleting the project $projectId from auth")
//        } catch (t: Throwable) {
//            logger.warn("Fail to delete the project $projectId from auth", t)
//        }
        projectPermissionService.deleteResource(projectId)
    }

    // 此处为兼容V0,V3并存的情况, 拉群用户有权限的项目列表需取V0+V3的并集, 无论啥集群, 都需取两个iam环境下的数据, 完全迁移完后可直接指向V3
    override fun getProjectFromAuth(userId: String?, accessToken: String?): List<String> {
        // 全部迁移完后,直接用此实现
//        val projectEnglishNames = projectPermissionService.getUserProjects(userId!!)
        val iamV0List = getV0UserProject(userId, accessToken)
        logger.info("$userId V0 project: $iamV0List")
        val projectList = mutableSetOf<String>()
        projectList.addAll(iamV0List)
        // 请求v3以及rbac的项目
        val iamList = getIamUserProject(userId!!)
        logger.info("$userId iam project: $iamList")

        if (iamList.isNotEmpty()) {
            projectList.addAll(iamList)
        }
        return projectList.toList()
    }

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        return
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request($request) with code ${response.code()}, " +
                        "message ${response.message()} and response $responseContent"
                )
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        return if (permission == AuthPermission.MANAGE) {
            bsAuthProjectApi.checkProjectManager(userId, bsPipelineAuthServiceCode, projectCode)
        } else {
            bsAuthProjectApi.checkProjectUser(userId, bsPipelineAuthServiceCode, projectCode)
        }
    }

    override fun modifyProjectAuthResource(
        projectInfo: TProjectRecord,
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        projectPermissionService.modifyResource(
            projectInfo = projectInfo,
            resourceUpdateInfo = resourceUpdateInfo
        )
    }

    override fun cancelCreateAuthProject(status: Int, projectCode: String): Boolean {
        return projectPermissionService.cancelCreateAuthProject(
            status = status,
            projectCode = projectCode
        )
    }

    override fun createRoleGroupApplication(
        userId: String,
        applicationInfo: ApplicationInfo,
        gradeManagerId: String
    ): Boolean {
        return projectPermissionService.createRoleGroupApplication(
            userId = userId,
            applicationInfo = applicationInfo,
            gradeManagerId = gradeManagerId
        )
    }

    fun getInfoByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record)
    }

    override fun hasCreatePermission(userId: String): Boolean {
        return true
    }

    override fun createExtProject(
        userId: String,
        projectCode: String,
        projectCreateInfo: ProjectCreateInfo,
        needAuth: Boolean,
        needValidate: Boolean,
        channel: ProjectChannelCode
    ): ProjectVO? {
        val projectVO = super.createExtProject(userId, projectCode, projectCreateInfo, needAuth, needValidate, channel)
        val routerTag = if (channel == ProjectChannelCode.GITCI) {
            streamTag
        } else if (channel == ProjectChannelCode.CODECC) {
            autoTag
        } else if (channel != ProjectChannelCode.AUTO) {
            autoTag
        } else {
            prodTag
        }
        // 根据项目channel路由项目
        val projectTagUpdate = ProjectTagUpdateDTO(
            routerTag = routerTag!!,
            projectCodeList = arrayListOf(projectCode),
            bgId = null,
            centerId = null,
            deptId = null,
            channel = null
        )
        projectTagService.updateTagByProject(
            projectTagUpdate
        )
        updateProjectProperties(userId, projectCode, ProjectProperties(PipelineAsCodeSettings(true)))
        return projectVO
    }

    override fun organizationMarkUp(
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail
    ): ProjectCreateInfo {
        val bgId = if (projectCreateInfo.bgId == 0L) userDeptDetail.bgId.toLong() else projectCreateInfo.bgId
        val deptId = if (projectCreateInfo.deptId == 0L) userDeptDetail.deptId.toLong() else projectCreateInfo.deptId
        val centerId = if (projectCreateInfo.centerId == 0L) {
            userDeptDetail.centerId.toLong()
        } else projectCreateInfo.centerId
        val bgName = projectCreateInfo.bgName.ifEmpty { userDeptDetail.bgName }
        val deptName = projectCreateInfo.deptName.ifEmpty { userDeptDetail.deptName }
        val centerName = projectCreateInfo.centerName.ifEmpty { userDeptDetail.centerName }

        return ProjectCreateInfo(
            projectName = projectCreateInfo.projectName,
            projectType = projectCreateInfo.projectType,
            secrecy = projectCreateInfo.secrecy,
            description = projectCreateInfo.description,
            kind = projectCreateInfo.kind,
            bgId = bgId,
            bgName = bgName,
            centerId = centerId,
            centerName = centerName,
            deptId = deptId,
            deptName = deptName,
            englishName = projectCreateInfo.englishName
        )
    }

    private fun getV0UserProject(userId: String?, accessToken: String?): List<String> {
        val token = if (accessToken.isNullOrEmpty()) {
            bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        } else {
            accessToken
        }
        val url = "$v0IamUrl/projects?access_token=$token&user_id=$userId"
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
            it.project_code
        }
    }

    private fun getIamUserProject(userId: String): List<String> {
        if (v3Tag.isBlank() && rbacTag.isBlank()) {
            return emptyList()
        }
        logger.info("getUserProject tag: v3Tag=$v3Tag|rbacTag=$rbacTag")
        val projectList: MutableList<String> = ArrayList()
        try {
            getIamProjectList(
                tag = v3Tag,
                projectList = projectList,
                userId = userId
            )
            getIamProjectList(
                tag = rbacTag,
                projectList = projectList,
                userId = userId
            )
        } catch (e: Exception) {
            // 为防止V0,V3发布存在时间差,导致项目列表拉取异常
            logger.warn("get iam Project fail $userId $e")
            return emptyList()
        }
        return projectList
    }

    private fun getIamProjectList(
        tag: String,
        projectList: MutableList<String>,
        userId: String
    ): List<String> {
        if (!tag.isBlank()) {
            val iamProjectList = bkTag.invokeByTag(tag) {
                client.get(ServiceProjectAuthResource::class).getUserProjects(
                    userId = userId,
                    token = tokenService.getSystemToken(null)!!
                ).data
            }
            if (iamProjectList != null) {
                projectList.addAll(iamProjectList)
            }
        }
        return projectList
    }

    override fun createProjectUser(projectId: String, createInfo: ProjectCreateUserInfo): Boolean {
        projectExtPermissionService.createUser2Project(
            createUser = createInfo.createUserId,
            projectCode = projectId,
            roleName = createInfo.roleName,
            roleId = createInfo.roleId,
            userIds = createInfo.userIds!!,
            checkManager = true
        )
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectServiceImpl::class.java)!!
    }
}
