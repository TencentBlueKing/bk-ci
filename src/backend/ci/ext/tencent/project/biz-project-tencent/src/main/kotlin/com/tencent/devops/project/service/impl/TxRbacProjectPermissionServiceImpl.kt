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
 *
 */

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectApprovalCallbackDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.listener.TxIamRbacCreateApplicationEvent
import com.tencent.devops.project.listener.TxIamRbacCreateEvent
import com.tencent.devops.project.pojo.ApplicationInfo
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.IamRbacService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class TxRbacProjectPermissionServiceImpl @Autowired constructor(
    val objectMapper: ObjectMapper,
    val projectDispatcher: ProjectDispatcher,
    val client: Client,
    val tokenService: ClientTokenService,
    val iamConfiguration: IamConfiguration,
    val iamManagerService: V2ManagerService,
    val projectApprovalCallbackDao: ProjectApprovalCallbackDao,
    val dslContext: DSLContext,
    val projectDao: ProjectDao,
    val iamRbacService: IamRbacService
) : ProjectPermissionService {

    @Value("\${iam.v0.url:#{null}}")
    private val v0IamUrl: String = ""

    // 校验用户是否是项目成员
    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode,
            group = null
        ).data
            ?: false
    }

    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        resourceCreateInfo: AuthProjectCreateInfo
    ): String {
        val needApproval = true
        val iamSubjectScopes = resourceCreateInfo.subjectScopes
        val userId = resourceCreateInfo.userId
        val reason = resourceCreateInfo.projectCreateInfo.description
        logger.info("createResources : $needApproval|$iamSubjectScopes|$userId|$reason")
        // todo 是否要同步创建V0项目
        checkParams(
            needApproval = needApproval!!,
            subjectScopes = resourceCreateInfo.projectCreateInfo.subjectScopes,
            authSecrecy = resourceCreateInfo.projectCreateInfo.authSecrecy
        )
        iamSubjectScopes.ifEmpty {
            listOf(SubjectScopeInfo(id = ALL_MEMBERS, type = ALL_MEMBERS, name = ALL_MEMBERS_NAME))
        }
        if (needApproval) {
            projectDispatcher.dispatch(
                TxIamRbacCreateApplicationEvent(
                    userId = userId,
                    retryCount = 0,
                    delayMills = 1000,
                    resourceRegisterInfo = resourceRegisterInfo,
                    projectId = "",
                    subjectScopes = iamSubjectScopes,
                    reason = reason
                )
            )
        } else {
            // 若不需要审批，则直接异步创建分级管理员和默认用户组
            projectDispatcher.dispatch(
                TxIamRbacCreateEvent(
                    userId = userId,
                    retryCount = 0,
                    delayMills = 1000,
                    resourceRegisterInfo = resourceRegisterInfo,
                    projectId = "",
                    iamProjectId = null,
                    subjectScopes = iamSubjectScopes
                )
            )
        }
        return ""
    }

    override fun deleteResource(projectCode: String) {
        // 资源都在接入方本地，无需删除iam侧数据
        return
    }

    override fun modifyResource(
        dbProjectInfo: TProjectRecord,
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        val needApproval = resourceUpdateInfo.needApproval
        val iamSubjectScopes = resourceUpdateInfo.iamSubjectScopes
        val projectCode = resourceUpdateInfo.projectUpdateInfo.englishName
        val projectName = resourceUpdateInfo.projectUpdateInfo.projectName
        val userId = resourceUpdateInfo.userId
        val approvalStatus = dbProjectInfo.approvalStatus
        val relationId = dbProjectInfo.relationId
        val reason = resourceUpdateInfo.projectUpdateInfo.description
        // todo 上线之前这里必须刷数据，要不会报错，因为拿出的数据库，可授权人员范围为空。
        // 数据库中的最大可授权人员范围
        val dbSubjectscopes = JsonUtil.to(
            dbProjectInfo.subjectScopes, object : TypeReference<ArrayList<SubjectScopeInfo>>() {}
        )
        logger.info(
            "Rbac modifyResource : $needApproval|$iamSubjectScopes|$projectCode|" +
                "$projectName|$dbSubjectscopes"
        )
        checkParams(
            needApproval = needApproval,
            subjectScopes = resourceUpdateInfo.projectUpdateInfo.subjectScopes,
            authSecrecy = resourceUpdateInfo.projectUpdateInfo.authSecrecy
        )
        iamSubjectScopes.ifEmpty {
            listOf(SubjectScopeInfo(id = ALL_MEMBERS, type = ALL_MEMBERS, name = ALL_MEMBERS_NAME))
        }
        val subjectScopesStr = objectMapper.writeValueAsString(iamSubjectScopes)
        if (approvalStatus == ProjectApproveStatus.CREATE_PENDING.status ||
            approvalStatus == ProjectApproveStatus.UPDATE_PENDING.status
        ) {
            throw OperationException("The project is under approval, modification is not allowed！")
        }
        // 编辑发起审批分为两种：1、项目已创建成功或修改审核通过，修改项目发起的审批。2、项目创建被拒绝审批通过，再次修改项目发起的审批
        if (approvalStatus == ProjectApproveStatus.CREATE_REJECT.status) {
            // 项目创建被拒绝审批通过，再次修改项目发起的审批，此时直接发起创建分级管理员申请
            if (!needApproval) {
                throw OperationException("Modifications must be made through the bkci client！")
            }
            projectDispatcher.dispatch(
                TxIamRbacCreateApplicationEvent(
                    userId = userId,
                    retryCount = 0,
                    delayMills = 1000,
                    resourceRegisterInfo = ResourceRegisterInfo(
                        resourceCode = projectCode,
                        resourceName = projectName
                    ),
                    projectId = "",
                    subjectScopes = iamSubjectScopes,
                    reason = reason
                )
            )
        } else {
            val isAuthSecrecyChange = dbProjectInfo.authSecrecy != resourceUpdateInfo.projectUpdateInfo.authSecrecy
            // todo 注意空处理，可能会出现未刷数据的出现问题
            val isSubjectScopesChange = (dbSubjectscopes.toSet() != iamSubjectScopes.toSet())
            logger.info("Rbac modifyResource :$isAuthSecrecyChange|$isSubjectScopesChange")
            // 若可授权人员范围和私密字段未改变，直接结束
            if (!isAuthSecrecyChange && !isSubjectScopesChange) {
                return
            }
            if (needApproval) {
                iamRbacService.updateGradeManagerApplication(
                    projectCode = projectCode,
                    projectName = projectName,
                    userId = userId,
                    iamSubjectScopes = iamSubjectScopes,
                    projectInfo = dbProjectInfo,
                    isAuthSecrecyChange = isAuthSecrecyChange,
                    isSubjectScopesChange = isSubjectScopesChange,
                    subjectScopesStr = subjectScopesStr
                )
            } else {
                if (approvalStatus != ProjectApproveStatus.CREATE_APPROVED.status) {
                    throw OperationException("Modifications must be made through the bkci client！")
                }
                // 若relationId为空，则表示并没有在Iam那边注册分级管理员，那么不需要去修改分级管理员
                if (!relationId.isNullOrEmpty()) {
                    iamRbacService.updateManager(
                        projectCode = projectCode,
                        projectName = projectName,
                        userId = userId,
                        iamSubjectScopes = iamSubjectScopes,
                        relationId = dbProjectInfo.relationId
                    )
                }
            }
        }
    }

    private fun checkParams(
        needApproval: Boolean,
        subjectScopes: List<SubjectScopeInfo>?,
        authSecrecy: Boolean?
    ) {
        if (needApproval && (subjectScopes!!.isEmpty() || authSecrecy == null)) {
            throw OperationException("The maximum authorized scope or auth secrecy cannot be empty!!")
        }
    }

    override fun getUserProjects(userId: String): List<String> {
        return client.get(ServiceProjectAuthResource::class).getUserProjects(
            token = tokenService.getSystemToken(null)!!,
            userId = userId
        ).data ?: emptyList()
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        // TODO:
        return emptyMap()
    }

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            action = permission.value,
            relationResourceType = null
        ).data ?: false
    }

    fun createResourcesToV0(
        userId: String,
        accessToken: String?,
        projectCreateInfo: ResourceRegisterInfo,
        userDeptDetail: UserDeptDetail?
    ): String {
        // 创建AUTH项目
        val authUrl = "$v0IamUrl/projects?access_token=$accessToken"
        val param: MutableMap<String, String> = mutableMapOf("project_code" to projectCreateInfo.resourceCode)
        if (userDeptDetail != null) {
            param["bg_id"] = userDeptDetail.bgId
            param["dept_id"] = userDeptDetail.deptId
            param["center_id"] = userDeptDetail.centerId
            logger.info("createProjectResources add org info $param")
        }
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val json = objectMapper.writeValueAsString(param)
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder().url(authUrl).post(requestBody).build()
        val responseContent = request(request, "调用权限中心创建项目失败")
        val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to create the project of response $responseContent")
            throw OperationException("调用权限中心V0创建项目失败: ${result.message}")
        }
        val authProjectForCreateResult = result.data
        return if (authProjectForCreateResult != null) {
            if (authProjectForCreateResult.project_id.isBlank()) {
                throw OperationException("权限中心创建V0的项目ID无效")
            }
            authProjectForCreateResult.project_id
        } else {
            logger.warn("Fail to get the project id from response $responseContent")
            throw OperationException("权限中心V0创建的项目ID无效")
        }
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request($request) with code ${response.code()} , " +
                        "message ${response.message()} and response $responseContent"
                )
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    override fun cancelCreateAuthProject(status: Int, projectCode: String): Boolean {
        var success = false
        if (status == ProjectApproveStatus.CREATE_PENDING.status ||
            status == ProjectApproveStatus.CREATE_REJECT.status
        ) {
            val callbackRecord = projectApprovalCallbackDao.getCallbackByEnglishName(
                dslContext = dslContext,
                projectCode = projectCode
            ) ?: throw OperationException("callback application is not exist!")
            success = iamManagerService.cancelCallbackApplication(callbackRecord.callbackId)
        }
        return success
    }

    override fun createRoleGroupApplication(
        userId: String,
        applicationInfo: ApplicationInfo,
        gradeManagerId: String
    ): Boolean {
        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.page = 1
        v2PageInfoDTO.pageSize = 10
        val searchName = "${applicationInfo.englishName}-$VIEW_PROJECT_PERMISSION_GROUP_NAME"
        val searchGroupDTO = SearchGroupDTO
            .builder()
            .name(searchName)
            .build()
        var viewProjectPermissionGroup: V2ManagerRoleGroupInfo? = null
        val permissionGroup = iamManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId, searchGroupDTO, v2PageInfoDTO
        )
        permissionGroup.results.forEach {
            if (it.name == searchName) {
                viewProjectPermissionGroup = it
            }
        }
        if (viewProjectPermissionGroup == null) {
            throw OperationException("View Project Permission Group can not be null!")
        }
        val groupId = viewProjectPermissionGroup!!.id
        val applicationInfo = ApplicationDTO.builder()
            .groupId(listOf(groupId)).applicant(userId)
            .reason(applicationInfo.reason)
            .expiredAt(applicationInfo.expireTime.toLong())
            .build()
        iamManagerService.createRoleGroupApplicationV2(applicationInfo)
        return true
    }

    override fun needApproval(needApproval: Boolean?): Boolean = true

    companion object {
        val logger = LoggerFactory.getLogger(TxRbacProjectPermissionServiceImpl::class.java)
        private const val SYSTEM_DEFAULT_NAME = "蓝盾"
        private const val ALL_MEMBERS = "*"
        private const val ALL_MEMBERS_NAME = "全体成员"
        private const val VIEW_PROJECT_PERMISSION_GROUP_NAME = "查看项目权限组"
    }
}
