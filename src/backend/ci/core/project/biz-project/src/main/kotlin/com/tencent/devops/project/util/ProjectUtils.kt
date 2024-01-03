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

package com.tencent.devops.project.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectVO

@Suppress("ALL")
object ProjectUtils {

    fun packagingBean(
        tProjectRecord: TProjectRecord,
        managePermission: Boolean? = null,
        showUserManageIcon: Boolean? = null,
        viewPermission: Boolean? = null,
        pipelineTemplateInstallPerm: Boolean? = null,
        projectOrganizationInfo: ProjectOrganizationInfo? = null
    ): ProjectVO {
        return with(tProjectRecord) {
            ProjectVO(
                /* 已经投产旧插件的使用字段兼容 */
                project_id = projectId,
                project_name = projectName,
                project_code = englishName ?: "",
                cc_app_id = ccAppId ?: 0,
                cc_app_name = ccAppName ?: "",
                hybrid_cc_app_id = hybridCcAppId,
                id = id,
                projectId = projectId ?: "",
                projectName = projectName,
                projectCode = englishName ?: "",
                projectType = projectType ?: 0,
                approvalStatus = approvalStatus ?: 0,
                approvalTime = if (approvalTime == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
                },
                approver = approver ?: "",
                bgId = getFinalOrganizationId(projectOrganizationInfo, bgId),
                bgName = getFinalOrganizationName(projectOrganizationInfo, bgName),
                ccAppId = ccAppId ?: 0,
                ccAppName = ccAppName ?: "",
                centerId = getFinalOrganizationId(projectOrganizationInfo, centerId),
                centerName = getFinalOrganizationName(projectOrganizationInfo, centerName),
                createdAt = DateTimeUtil.toDateTime(createdAt, "yyyy-MM-dd"),
                creator = creator ?: "",
                dataId = dataId ?: 0,
                deployType = deployType ?: "",
                deptId = getFinalOrganizationId(projectOrganizationInfo, deptId),
                deptName = getFinalOrganizationName(projectOrganizationInfo, deptName),
                businessLineId = getFinalOrganizationId(projectOrganizationInfo, businessLineId),
                businessLineName = getFinalOrganizationName(projectOrganizationInfo, businessLineName),
                description = description ?: "",
                englishName = englishName ?: "",
                extra = extra ?: "",
                offlined = isOfflined,
                secrecy = isSecrecy,
                helmChartEnabled = isHelmChartEnabled,
                kind = kind,
                logoAddr = logoAddr ?: "",
                remark = remark ?: "",
                updator = updator,
                updatedAt = if (updatedAt == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(updatedAt, "yyyy-MM-dd")
                },
                useBk = useBk,
                enabled = enabled ?: true,
                gray = routerTag == "gray",
                hybridCcAppId = hybridCcAppId,
                enableExternal = enableExternal,
                pipelineLimit = pipelineLimit,
                routerTag = routerTag,
                relationId = relationId,
                properties = properties?.let { self ->
                    JsonUtil.to(self, ProjectProperties::class.java)
                },
                subjectScopes = subjectScopes?.let {
                    JsonUtil.to(it, object : TypeReference<List<SubjectScopeInfo>>() {})
                },
                authSecrecy = authSecrecy,
                managePermission = managePermission,
                showUserManageIcon = showUserManageIcon,
                channelCode = channel,
                productId = productId,
                canView = viewPermission,
                pipelineTemplateInstallPerm = pipelineTemplateInstallPerm
            )
        }
    }

    fun packagingBean(
        tProjectRecord: TProjectRecord,
        projectApprovalInfo: ProjectApprovalInfo?,
        projectOrganizationInfo: ProjectOrganizationInfo? = null
    ): ProjectDiffVO {
        val subjectScopes = tProjectRecord.subjectScopes?.let {
            JsonUtil.to(it, object : TypeReference<ArrayList<SubjectScopeInfo>>() {})
        }
        return with(tProjectRecord) {
            ProjectDiffVO(
                id = id,
                projectId = projectId,
                projectName = projectName,
                afterProjectName = projectApprovalInfo?.projectName ?: projectName,
                projectCode = englishName ?: "",
                approvalStatus = approvalStatus ?: 0,
                approvalTime = if (approvalTime == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
                },
                approver = approver ?: "",
                bgId = getFinalOrganizationId(projectOrganizationInfo, bgId),
                afterBgId = projectApprovalInfo?.bgId ?: bgId?.toString(),
                bgName = getFinalOrganizationName(projectOrganizationInfo, bgName),
                afterBgName = projectApprovalInfo?.bgName ?: bgName ?: "",
                businessLineId = getFinalOrganizationId(projectOrganizationInfo, businessLineId),
                afterBusinessLineId = projectApprovalInfo?.businessLineId ?: businessLineId,
                businessLineName = getFinalOrganizationName(projectOrganizationInfo, businessLineName),
                afterBusinessLineName = projectApprovalInfo?.businessLineName ?: businessLineName,
                centerId = getFinalOrganizationId(projectOrganizationInfo, centerId),
                afterCenterId = projectApprovalInfo?.centerId ?: centerId?.toString(),
                centerName = getFinalOrganizationName(projectOrganizationInfo, centerName),
                afterCenterName = projectApprovalInfo?.centerName ?: centerName ?: "",
                createdAt = DateTimeUtil.toDateTime(createdAt, "yyyy-MM-dd"),
                creator = creator ?: "",
                deptId = getFinalOrganizationId(projectOrganizationInfo, deptId),
                afterDeptId = projectApprovalInfo?.deptId ?: deptId?.toString(),
                deptName = getFinalOrganizationName(projectOrganizationInfo, deptName),
                afterDeptName = projectApprovalInfo?.deptName ?: deptName ?: "",
                description = description ?: "",
                afterDescription = projectApprovalInfo?.description ?: description ?: "",
                englishName = englishName ?: "",
                logoAddr = logoAddr ?: "",
                afterLogoAddr = projectApprovalInfo?.logoAddr ?: logoAddr ?: "",
                remark = remark ?: "",
                updator = updator,
                updatedAt = if (updatedAt == null) {
                    ""
                } else {
                    DateTimeUtil.toDateTime(updatedAt, "yyyy-MM-dd")
                },
                subjectScopes = subjectScopes,
                afterSubjectScopes = projectApprovalInfo?.subjectScopes ?: subjectScopes,
                authSecrecy = authSecrecy,
                afterAuthSecrecy = projectApprovalInfo?.authSecrecy ?: authSecrecy,
                projectType = projectType,
                afterProjectType = projectApprovalInfo?.projectType,
                productId = productId,
                afterProductId = projectApprovalInfo?.productId
            )
        }
    }

    private fun getFinalOrganizationId(
        projectOrganizationInfo: ProjectOrganizationInfo?,
        OrganizationIdInDb: Long
    ): String? {
        return (if (projectOrganizationInfo != null) projectOrganizationInfo.deptId else OrganizationIdInDb)?.toString()
    }

    private fun getFinalOrganizationName(
        projectOrganizationInfo: ProjectOrganizationInfo?,
        OrganizationNameInDb: String
    ): String {
        return (if (projectOrganizationInfo != null) projectOrganizationInfo.deptName else OrganizationNameInDb) ?: ""
    }
}
