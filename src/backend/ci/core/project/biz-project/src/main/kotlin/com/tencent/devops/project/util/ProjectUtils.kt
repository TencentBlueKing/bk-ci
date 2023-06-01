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
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectVO

@Suppress("ALL")
object ProjectUtils {

    fun packagingBean(
        tProjectRecord: TProjectRecord,
        managePermission: Boolean? = null,
        showUserManageIcon: Boolean? = null
    ): ProjectVO {
        return ProjectVO(
            /* 已经投产旧插件的使用字段兼容 */
            project_id = tProjectRecord.projectId,
            project_name = tProjectRecord.projectName,
            project_code = tProjectRecord.englishName ?: "",
            cc_app_id = tProjectRecord.ccAppId ?: 0,
            cc_app_name = tProjectRecord.ccAppName ?: "",
            hybrid_cc_app_id = tProjectRecord.hybridCcAppId,
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
            englishName = tProjectRecord.englishName ?: "",
            extra = tProjectRecord.extra ?: "",
            offlined = tProjectRecord.isOfflined,
            secrecy = tProjectRecord.isSecrecy,
            helmChartEnabled = tProjectRecord.isHelmChartEnabled,
            kind = tProjectRecord.kind,
            logoAddr = tProjectRecord.logoAddr ?: "",
            remark = tProjectRecord.remark ?: "",
            updator = tProjectRecord.updator,
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            useBk = tProjectRecord.useBk,
            enabled = tProjectRecord.enabled ?: true,
            gray = tProjectRecord.routerTag == "gray",
            hybridCcAppId = tProjectRecord.hybridCcAppId,
            enableExternal = tProjectRecord.enableExternal,
            pipelineLimit = tProjectRecord.pipelineLimit,
            routerTag = tProjectRecord.routerTag,
            relationId = tProjectRecord.relationId,
            properties = tProjectRecord.properties?.let { self ->
                JsonUtil.to(self, ProjectProperties::class.java)
            },
            subjectScopes = tProjectRecord.subjectScopes?.let {
                JsonUtil.to(it, object : TypeReference<List<SubjectScopeInfo>>() {})
            },
            authSecrecy = tProjectRecord.authSecrecy,
            managePermission = managePermission,
            showUserManageIcon = showUserManageIcon,
            channelCode = tProjectRecord.channel
        )
    }

    fun packagingBean(tProjectRecord: TProjectRecord, projectApprovalInfo: ProjectApprovalInfo?): ProjectDiffVO {
        val subjectScopes = tProjectRecord.subjectScopes?.let {
            JsonUtil.to(it, object : TypeReference<ArrayList<SubjectScopeInfo>>() {})
        }
        return ProjectDiffVO(
            id = tProjectRecord.id,
            projectId = tProjectRecord.projectId,
            projectName = tProjectRecord.projectName,
            afterProjectName = projectApprovalInfo?.projectName ?: tProjectRecord.projectName,
            projectCode = tProjectRecord.englishName ?: "",
            approvalStatus = tProjectRecord.approvalStatus ?: 0,
            approvalTime = if (tProjectRecord.approvalTime == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
            },
            approver = tProjectRecord.approver ?: "",
            bgId = tProjectRecord.bgId?.toString(),
            afterBgId = projectApprovalInfo?.bgId ?: tProjectRecord.bgId?.toString(),
            bgName = tProjectRecord.bgName ?: "",
            afterBgName = projectApprovalInfo?.bgName ?: tProjectRecord.bgName ?: "",
            centerId = tProjectRecord.centerId?.toString(),
            afterCenterId = projectApprovalInfo?.centerId ?: tProjectRecord.centerId?.toString(),
            centerName = tProjectRecord.centerName ?: "",
            afterCenterName = projectApprovalInfo?.centerName ?: tProjectRecord.centerName ?: "",
            createdAt = DateTimeUtil.toDateTime(tProjectRecord.createdAt, "yyyy-MM-dd"),
            creator = tProjectRecord.creator ?: "",
            deptId = tProjectRecord.deptId?.toString(),
            afterDeptId = projectApprovalInfo?.deptId ?: tProjectRecord.deptId?.toString(),
            deptName = tProjectRecord.deptName ?: "",
            afterDeptName = projectApprovalInfo?.deptName ?: tProjectRecord.deptName ?: "",
            description = tProjectRecord.description ?: "",
            afterDescription = projectApprovalInfo?.description ?: tProjectRecord.description ?: "",
            englishName = tProjectRecord.englishName ?: "",
            logoAddr = tProjectRecord.logoAddr ?: "",
            afterLogoAddr = projectApprovalInfo?.logoAddr ?: tProjectRecord.logoAddr ?: "",
            remark = tProjectRecord.remark ?: "",
            updator = tProjectRecord.updator,
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            subjectScopes = subjectScopes,
            afterSubjectScopes = projectApprovalInfo?.subjectScopes ?: subjectScopes,
            authSecrecy = tProjectRecord.authSecrecy,
            afterAuthSecrecy = projectApprovalInfo?.authSecrecy ?: tProjectRecord.authSecrecy,
            projectType = tProjectRecord.projectType,
            afterProjectType = projectApprovalInfo?.projectType
        )
    }
}
