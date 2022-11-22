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
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationUpdateDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectApprovalCallbackDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.listener.TxIamRbacCreateEvent
import com.tencent.devops.project.listener.TxIamRbacCreateApplicationEvent
import com.tencent.devops.project.pojo.ApplicationInfo
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.ResourceCreateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.pojo.enums.ApproveType
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.AuthorizationUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class TxRbacProjectPermissionServiceImpl @Autowired constructor(
    val objectMapper: ObjectMapper,
    val authProperties: BkAuthProperties,
    val projectDispatcher: ProjectDispatcher,
    val client: Client,
    val tokenService: ClientTokenService,
    val iamConfiguration: IamConfiguration,
    val iamManagerService: V2ManagerService,
    val projectApprovalCallbackDao: ProjectApprovalCallbackDao,
    val dslContext: DSLContext,
    val projectDao: ProjectDao
) : ProjectPermissionService {

    @Value("\${iam.v0.url:#{null}}")
    private val v0IamUrl: String = ""

    @Value("\${itsm.callback.url.update:#{null}}")
    private val itsmUpdateCallBackUrl: String = ""

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
        resourceCreateInfo: ResourceCreateInfo
    ): String {
        val needApproval = resourceCreateInfo.needApproval
        val iamSubjectScopes = resourceCreateInfo.iamSubjectScopes
        val userId = resourceCreateInfo.userId
        val reason = resourceCreateInfo.projectCreateInfo.description
        logger.info("createResources : $needApproval|$iamSubjectScopes|$userId|$reason")
        // todo 是否要同步创建V0项目
        checkParams(
            needApproval = needApproval!!,
            subjectScopes = resourceCreateInfo.projectCreateInfo.subjectScopes,
            authSecrecy = resourceCreateInfo.projectCreateInfo.authSecrecy
        )
        if (iamSubjectScopes.isEmpty()) {
            iamSubjectScopes.add(ManagerScopes(
                ManagerScopesEnum.getType(ManagerScopesEnum.ALL),
                ALL_MEMBERS
            ))
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
        projectInfo: TProjectRecord,
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        val needApproval = resourceUpdateInfo.needApproval
        val iamSubjectScopes = resourceUpdateInfo.iamSubjectScopes
        val projectCode = resourceUpdateInfo.projectUpdateInfo.englishName
        val projectName = resourceUpdateInfo.projectUpdateInfo.projectName
        val userId = resourceUpdateInfo.userId
        val approvalStatus = projectInfo.approvalStatus
        val reason = resourceUpdateInfo.projectUpdateInfo.description
        // 数据库中的最大可授权人员范围
        val dbSubjectscopes = JsonUtil.to(
            projectInfo.subjectscopes, object : TypeReference<ArrayList<ManagerScopes>>() {}
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
        if (iamSubjectScopes.isEmpty()) {
            iamSubjectScopes.add(ManagerScopes(
                ManagerScopesEnum.getType(ManagerScopesEnum.ALL),
                ALL_MEMBERS
            ))
        }
        val subjectScopesStr = objectMapper.writeValueAsString(iamSubjectScopes)
        if (approvalStatus == ApproveStatus.CREATE_PENDING.status
            || approvalStatus == ApproveStatus.UPDATE_PENDING.status
        ) {
            throw OperationException("The project is under approval, modification is not allowed！")
        }
        // 编辑发起审批分为两种：1、项目已创建成功或修改审核通过，修改项目发起的审批。2、项目创建被拒绝审批通过，再次修改项目发起的审批
        if (approvalStatus == ApproveStatus.CREATE_REJECT.status) {
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
            val isAuthSecrecyChange = projectInfo.isAuthSecrecy != resourceUpdateInfo.projectUpdateInfo.authSecrecy
            val isSubjectScopesChange = (dbSubjectscopes.toSet() != iamSubjectScopes.toSet())
            logger.info("Rbac modifyResource :$isAuthSecrecyChange|$isAuthSecrecyChange")
            // 若可授权人员范围和私密字段未改变，直接结束
            if (!isAuthSecrecyChange && !isSubjectScopesChange) {
                return
            }
            if (needApproval) {
                updateGradeManagerApplication(
                    projectCode = projectCode,
                    projectName = projectName,
                    userId = userId,
                    iamSubjectScopes = iamSubjectScopes,
                    projectInfo = projectInfo,
                    isAuthSecrecyChange = isAuthSecrecyChange,
                    isSubjectScopesChange = isSubjectScopesChange,
                    subjectScopesStr = subjectScopesStr
                )
            } else {
                if (approvalStatus != ApproveStatus.CREATE_APPROVED.status) {
                    throw OperationException("Modifications must be made through the bkci client！")
                }
                updateManager(
                    projectCode = projectCode,
                    projectName = projectName,
                    userId = userId,
                    iamSubjectScopes = iamSubjectScopes,
                    relationId = projectInfo.relationId
                )
            }
        }
    }

    private fun updateManager(
        projectCode: String,
        projectName: String,
        userId: String,
        iamSubjectScopes: List<ManagerScopes>,
        relationId: String
    ) {
        val authorizationScopes = AuthorizationUtils.buildManagerResources(
            projectId = projectCode,
            projectName = projectName,
            iamConfiguration = iamConfiguration
        )
        val updateManagerDTO: UpdateManagerDTO = UpdateManagerDTO.builder()
            .name("$SYSTEM_DEFAULT_NAME-$projectName")
            .description(IamGroupUtils.buildManagerUpdateDescription(projectCode, userId))
            .authorizationScopes(authorizationScopes)
            .subjectScopes(iamSubjectScopes)
            .build()
        logger.info("updateManager : $updateManagerDTO")
        iamManagerService.updateManagerV2(relationId, updateManagerDTO)
    }

    private fun updateGradeManagerApplication(
        projectCode: String,
        projectName: String,
        userId: String,
        iamSubjectScopes: List<ManagerScopes>,
        projectInfo: TProjectRecord,
        isAuthSecrecyChange: Boolean,
        isSubjectScopesChange: Boolean,
        subjectScopesStr: String
    ) {
        val authorizationScopes = AuthorizationUtils.buildManagerResources(
            projectId = projectCode,
            projectName = projectName,
            iamConfiguration = iamConfiguration
        )
        val callbackId = UUIDUtil.generate()
        val gradeManagerApplicationUpdateDTO = GradeManagerApplicationUpdateDTO.builder()
            .name("$SYSTEM_DEFAULT_NAME-$projectName")
            .description(IamGroupUtils.buildManagerUpdateDescription(projectCode, userId))
            .authorizationScopes(authorizationScopes)
            .subjectScopes(iamSubjectScopes)
            .syncPerm(true)
            .applicant(userId)
            .reason(IamGroupUtils.buildManagerUpdateDescription(projectCode, userId))
            .callbackId(callbackId)
            // todo 需补充
            .callbackUrl(itsmUpdateCallBackUrl)
            .content("xxx")
            .title("xxx")
            .build()
        logger.info("gradeManagerApplicationUpdateDTO : $gradeManagerApplicationUpdateDTO")
        val updateGradeManagerApplication =
            iamManagerService.updateGradeManagerApplication(projectInfo.relationId, gradeManagerApplicationUpdateDTO)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val approveType = if (isAuthSecrecyChange && isSubjectScopesChange) ApproveType.ALL_CHANGE_APPROVE.type
            else if (isSubjectScopesChange) ApproveType.SUBJECT_SCOPES_APPROVE.type
            else ApproveType.AUTH_SECRECY_APPROVE.type
            logger.info("approveType : $approveType")
            // 修改项目状态
            projectDao.updateProjectStatusByEnglishName(
                dslContext = context,
                projectCode = projectInfo.englishName,
                statusEnum = ApproveStatus.UPDATE_PENDING
            )
            // 存储审批单
            projectApprovalCallbackDao.create(
                dslContext = context,
                applicant = userId,
                englishName = projectCode,
                callbackId = callbackId,
                sn = updateGradeManagerApplication.sn,
                subjectScopes = subjectScopesStr,
                approveType = approveType
            )
        }
    }

    private fun checkParams(
        needApproval: Boolean,
        subjectScopes: ArrayList<ManagerScopes>?,
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
        if (status == ApproveStatus.CREATE_PENDING.status) {
            val callbackRecord = projectApprovalCallbackDao.getCallbackByEnglishName(
                dslContext = dslContext,
                projectCode = projectCode
            ) ?: throw OperationException("callback application is not exist!")
            success = iamManagerService.cancelCallbackApplication(callbackRecord.callbackId)
        }
        return success
    }

    override fun createRoleGroupApplication(userId: String, applicationInfo: ApplicationInfo): Boolean {
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxRbacProjectPermissionServiceImpl::class.java)
        private const val SYSTEM_DEFAULT_NAME = "蓝盾"
        private const val ALL_MEMBERS = "*"
    }
}
