package com.tencent.devops.project.util

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.ProjectVO

object ProjectUtils {

    fun packagingBean(tProjectRecord: TProjectRecord, grayProjectSet: Set<String>): ProjectVO {
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
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            useBk = tProjectRecord.useBk,
            enabled = tProjectRecord.enabled ?: true,
            gray = grayProjectSet.contains(tProjectRecord.englishName),
            hybridCcAppId = tProjectRecord.hybridCcAppId,
            enableExternal = tProjectRecord.enableExternal
        )
    }
}
