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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationCreateDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.itsm.ItsmAttrs
import com.tencent.bk.sdk.iam.dto.itsm.ItsmColumn
import com.tencent.bk.sdk.iam.dto.itsm.ItsmContentDTO
import com.tencent.bk.sdk.iam.dto.itsm.ItsmScheme
import com.tencent.bk.sdk.iam.dto.itsm.ItsmStyle
import com.tencent.bk.sdk.iam.dto.itsm.ItsmValue
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.config.ItsmConfig
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.ResourceCreateInfo
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS_NAME
import com.tencent.devops.common.auth.callback.AuthConstants.USER_TYPE
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import java.util.Arrays
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PermissionGradeManagerService @Autowired constructor(
    private val client: Client,
    private val permissionScopesService: PermissionScopesService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val itsmConfig: ItsmConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionGradeManagerService::class.java)
        private const val DEPARTMENT = "department"
        private const val DEPARTMENT_TYPE = "depart"
    }

    /**
     * 创建分级管理员
     */
    @SuppressWarnings("LongParameterList")
    fun createGradeManager(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val projectApprovalInfo = client.get(ServiceProjectApprovalResource::class).get(projectId = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                params = arrayOf(projectCode),
                defaultMessage = "the resource not exists, projectCode:$projectCode"
            )
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = resourceName,
        )
        val description = IamGroupUtils.buildManagerDescription(
            projectName = resourceName,
            userId = userId
        )
        val authorizationScopes = permissionScopesService.buildGradeManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName
        )
        val subjectScopes = projectApprovalInfo.subjectScopes?.map {
            when (it.type) {
                DEPARTMENT_TYPE -> ManagerScopes(DEPARTMENT, it.id)
                USER_TYPE -> ManagerScopes(it.type, it.name)
                else -> ManagerScopes(it.type, it.id)
            }
        } ?: listOf(ManagerScopes(ALL_MEMBERS, ALL_MEMBERS))
        val createManagerDTO = CreateManagerDTO.builder()
            .system(iamConfiguration.systemId)
            .name(name)
            .description(description)
            .members(listOf(userId))
            .authorization_scopes(authorizationScopes)
            .subject_scopes(subjectScopes)
            .sync_perm(true)
            .build()
        val gradeManagerId = iamV2ManagerService.createManagerV2(createManagerDTO)
        permissionResourceGroupService.createGradeDefaultGroup(
            gradeManagerId = gradeManagerId,
            userId = userId,
            projectCode = projectCode,
            projectName = projectName
        )
        return gradeManagerId
    }

    private fun createGradeManagerApplication(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCreateInfo: ResourceCreateInfo?
    ) {
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectCode),
                defaultMessage = "项目[$projectCode]不存在"
            )
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = projectName,
        )
        val description = IamGroupUtils.buildManagerDescription(
            projectName = projectName,
            userId = userId
        )
        val authorizationScopes = permissionScopesService.buildGradeManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName
        )
        val subjectScopes = resourceCreateInfo?.subjectScopes?.map {
            when (it.type) {
                DEPARTMENT_TYPE -> ManagerScopes(DEPARTMENT, it.id)
                USER_TYPE -> ManagerScopes(it.type, it.name)
                else -> ManagerScopes(it.type, it.id)
            }
        } ?: listOf(ManagerScopes(ALL_MEMBERS, ALL_MEMBERS))
        val callbackId = UUIDUtil.generate()

        val itsmContentDTO = buildItsmContentDTO(
            projectName = projectName,
            projectId = projectCode,
            desc = projectInfo.description ?: "",
            organization = "${projectInfo.bgName}-${projectInfo.deptName}-${projectInfo.deptName}",
            authSecrecy = resourceCreateInfo?.authSecrecy ?: false,
            subjectScopes = resourceCreateInfo?.subjectScopes ?: listOf(
                SubjectScopeInfo(
                    id = ALL_MEMBERS,
                    type = ALL_MEMBERS,
                    name = ALL_MEMBERS_NAME
                )
            )
        )
        val gradeManagerApplicationCreateDTO = GradeManagerApplicationCreateDTO
            .builder()
            .name(name)
            .description(description)
            .members(arrayListOf(userId))
            .authorizationScopes(authorizationScopes)
            .subjectScopes(subjectScopes)
            .syncPerm(true)
            .applicant(userId)
            .reason(IamGroupUtils.buildItsmDefaultReason(projectName, projectCode, true))
            .callbackId(callbackId)
            .callbackUrl(String.format(itsmConfig.itsmCreateCallBackUrl, projectCode))
            .content(itsmContentDTO)
            .title("创建蓝盾项目申请")
            .build()
        logger.info("gradeManagerApplicationCreateDTO : $gradeManagerApplicationCreateDTO")
        val createGradeManagerApplication =
            iamV2ManagerService.createGradeManagerApplication(gradeManagerApplicationCreateDTO)
    }

    @Suppress("LongParameterList")
    private fun buildItsmContentDTO(
        projectName: String,
        projectId: String,
        desc: String,
        organization: String,
        authSecrecy: Boolean,
        subjectScopes: List<SubjectScopeInfo>
    ): ItsmContentDTO {
        val itsmColumns = listOf(
            ItsmColumn.builder().key("projectName").name("项目名称").type("text").build(),
            ItsmColumn.builder().key("projectId").name("项目ID").type("text").build(),
            ItsmColumn.builder().key("desc").name("项目描述").type("text").build(),
            ItsmColumn.builder().key("organization").name("所属组织").type("text").build(),
            ItsmColumn.builder().key("authSecrecy").name("项目性质").type("text").build(),
            ItsmColumn.builder().key("subjectScopes").name("最大可授权人员范围").type("text").build()
        )
        val itsmAttrs = ItsmAttrs.builder().column(itsmColumns).build()
        val itsmScheme = ItsmScheme.builder().attrs(itsmAttrs).type("table").build()
        val scheme = HashMap<String, ItsmScheme>()
        scheme["content_table"] = itsmScheme
        val value = HashMap<String, ItsmStyle>()
        value["projectName"] = ItsmStyle.builder().value(projectName).build()
        value["projectId"] = ItsmStyle.builder().value(projectId).build()
        value["desc"] = ItsmStyle.builder().value(desc).build()
        value["organization"] = ItsmStyle.builder().value(organization).build()
        value["authSecrecy"] = ItsmStyle.builder().value(if (authSecrecy) "私密项目" else "公开项目").build()
        value["subjectScopes"] = ItsmStyle.builder().value(JsonUtil.toJson(subjectScopes)).build()
        val itsmValue = ItsmValue.builder().scheme("content_table").lable("项目创建审批").value(listOf(value)).build()
        return ItsmContentDTO.builder().formData(Arrays.asList(itsmValue)).schemes(scheme).build()
    }

    /**
     * 修改分级管理员
     */
    @SuppressWarnings("LongParameterList")
    fun modifyGradeManager(
        gradeManagerId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ) {
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = resourceName,
        )
        val authorizationScopes = permissionScopesService.buildGradeManagerAuthorizationScopes(
            strategyName = IamGroupUtils.buildGroupStrategyName(
                resourceType = resourceType,
                groupCode = DefaultGroupType.MANAGER.value
            ),
            projectCode = projectCode,
            projectName = projectName
        )
        val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(gradeManagerId)
        val updateManagerDTO = UpdateManagerDTO.builder()
            .name(name)
            .members(gradeManagerDetail.members)
            .description(gradeManagerDetail.description)
            .authorizationScopes(authorizationScopes)
            .subjectScopes(listOf(ManagerScopes("*", "*")))
            .syncPerm(true)
            .build()
        iamV2ManagerService.updateManagerV2(gradeManagerId, updateManagerDTO)
    }

    fun deleteGradeManager(gradeManagerId: String) {
        iamV2ManagerService.deleteManagerV2(gradeManagerId)
    }

    fun listGroup(
        gradeManagerId: String
    ): List<IamGroupInfoVo> {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 10
        val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId,
            null,
            pageInfoDTO
        )
        return iamGroupInfoList.results.map {
            IamGroupInfoVo(
                id = it.id,
                name = it.name,
                displayName = IamGroupUtils.getGroupDisplayName(it.name),
                userCount = it.userCount,
                departmentCount = it.departmentCount
            )
        }.sortedBy { it.id }
    }
}
