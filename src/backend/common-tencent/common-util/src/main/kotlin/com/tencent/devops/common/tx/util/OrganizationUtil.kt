package com.tencent.devops.common.tx.util

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT

object OrganizationUtil {
    data class Organization(
        val bgName: String,
        val deptName: String,
        val centerName: String
    )

    /**
     * 根据不同类型的组织内容组装出统一的三级组织
     */
    fun fillOrganization(
        organizationType: String,
        organizationName: String,
        deptName: String?,
        centerName: String?
    ): Organization {
        var realBgName = ""
        var realDeptName = deptName
        var realCenterName = centerName
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> {
                realBgName = organizationName
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> {
                realDeptName = organizationName
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> {
                realDeptName = ""
                realCenterName = organizationName
            }
        }
        return Organization(realBgName, realDeptName
            ?: "", realCenterName ?: "")
    }
}