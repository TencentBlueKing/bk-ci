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
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectApprovalService
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.ProjectExtService
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
    dslContext: DSLContext,
    projectDao: ProjectDao,
    private val tofService: TOFService,
    private val bkRepoClient: BkRepoClient,
    private val projectPaasCCService: ProjectPaasCCService,
    private val authProjectApi: AuthProjectApi,
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
    objectMapper: ObjectMapper,
    projectExtService: ProjectExtService,
    projectApprovalService: ProjectApprovalService
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
    objectMapper = objectMapper,
    projectExtService = projectExtService,
    projectApprovalService = projectApprovalService
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

    override fun getByEnglishName(
        userId: String,
        englishName: String,
        accessToken: String?
    ): ProjectVO? {
        val projectVO = getInfoByEnglishName(userId = userId, englishName = englishName)
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
        val isNotCreateSuccess = projectVO.creator == userId &&
            (projectVO.approvalStatus == ProjectApproveStatus.CREATE_PENDING.status ||
                projectVO.approvalStatus == ProjectApproveStatus.CREATE_REJECT.status)
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

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val serviceUrl =
            "$serviceUrlPrefix/service/file/upload?userId=$userId"
        OkhttpUtils.uploadFile(serviceUrl, logoFile).use { response ->
            val responseContent = response.body!!.string()
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
        projectPermissionService.deleteResource(projectId)
    }

    override fun getProjectFromAuth(
        userId: String?,
        accessToken: String?
    ): List<String> {
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

    override fun getProjectFromAuth(
        userId: String,
        accessToken: String?,
        permission: AuthPermission
    ): List<String>? {
        if (rbacTag.isBlank()) {
            return emptyList()
        }
        return bkTag.invokeByTag(rbacTag) {
            client.getGateway(ServiceProjectAuthResource::class).getUserProjectsByPermission(
                userId = userId,
                token = tokenService.getSystemToken(null)!!,
                action = permission.value
            ).data
        }
    }

    override fun isShowUserManageIcon(routerTag: String?): Boolean =
        routerTag?.contains(rbacTag) ?: false

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        return
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request($request) with code ${response.code}, " +
                        "message ${response.message} and response $responseContent"
                )
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        return authProjectApi.validateUserProjectPermission(
            user = userId,
            serviceCode = bsPipelineAuthServiceCode,
            permission = permission,
            projectCode = projectCode
        )
    }

    override fun modifyProjectAuthResource(
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        projectPermissionService.modifyResource(
            resourceUpdateInfo = resourceUpdateInfo
        )
    }

    override fun cancelCreateAuthProject(userId: String, projectCode: String) {
        projectPermissionService.cancelCreateAuthProject(userId = userId, projectCode = projectCode)
    }

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) {
        projectPermissionService.cancelUpdateAuthProject(userId = userId, projectCode = projectCode)
    }

    fun getInfoByEnglishName(userId: String, englishName: String): ProjectVO? {
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

        return projectCreateInfo.copy(
            bgId = bgId,
            bgName = bgName,
            centerId = centerId,
            centerName = centerName,
            deptId = deptId,
            deptName = deptName
        )
    }

    override fun buildRouterTag(routerTag: String?): String {
        return if (routerTag == null) {
            AuthSystemType.V0_AUTH_TYPE.value
        } else if (routerTag.contains(AuthSystemType.V3_AUTH_TYPE.value)) {
            AuthSystemType.V3_AUTH_TYPE.value
        } else if (routerTag.contains(AuthSystemType.RBAC_AUTH_TYPE.value)) {
            AuthSystemType.RBAC_AUTH_TYPE.value
        } else {
            AuthSystemType.V0_AUTH_TYPE.value
        }
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
        val responseContent = request(request, I18nUtil.getCodeLanMessage(
            messageCode = ProjectMessageCode.PEM_QUERY_ERROR,
            language = I18nUtil.getLanguage(userId)))
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException(I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.PEM_QUERY_ERROR,
                language = I18nUtil.getLanguage(userId)))
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
        val projectList = mutableListOf<String>()
        try {
            getIamProjectList(
                tag = v3Tag,
                projectList = projectList,
                userId = userId
            )
            logger.info("get v3 Project $projectList")
            getIamProjectList(
                tag = rbacTag,
                projectList = projectList,
                userId = userId
            )
            logger.info("get rbac+v3 Project $projectList")
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
                client.getGateway(ServiceProjectAuthResource::class).getUserProjects(
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

    override fun isRbacPermission(projectId: String): Boolean {
        val projectInfo = projectDao.getByEnglishName(
            dslContext = dslContext,
            englishName = projectId
        ) ?: return false
        return projectInfo.routerTag?.contains(rbacTag) ?: false
    }

    override fun updateProjectRouterTag(englishName: String) {
        try {
            val tag = bkTag.getLocalTag()
            // rbac环境创建的项目,需要指定到rbac集群
            if (tag.contains(rbacTag)) {
                projectTagService.updateTagByProject(projectCode = englishName, tag = rbacTag)
            }
        } catch (ignore: Exception) {
            logger.warn("Failed to update project router tag", ignore)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectServiceImpl::class.java)!!
    }
}
