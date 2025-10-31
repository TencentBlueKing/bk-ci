/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceAuthorizationService

class SamplePermissionMigrateService(
    val migrateResourceAuthorizationService: MigrateResourceAuthorizationService
) : PermissionMigrateService {
    override fun v3ToRbacAuth(projectCodes: List<String>): Boolean {
        return true
    }

    override fun v0ToRbacAuth(projectCodes: List<String>): Boolean {
        return true
    }

    override fun allToRbacAuth(): Boolean {
        return true
    }

    override fun toRbacAuthByCondition(projectConditionDTO: ProjectConditionDTO): Boolean {
        return true
    }

    override fun compareResult(projectCode: String): Boolean {
        return true
    }

    override fun resetProjectPermissions(migrateResourceDTO: MigrateResourceDTO): Boolean {
        return true
    }

    override fun resetPermissionsWhenEnabledProject(projectCode: String): Boolean {
        return true
    }

    override fun grantGroupAdditionalAuthorization(projectCodes: List<String>): Boolean {
        return true
    }

    override fun handoverAllPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean {
        return true
    }

    override fun handoverPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean {
        return true
    }

    override fun migrateMonitorResource(
        projectCodes: List<String>,
        asyncMigrateManagerGroup: Boolean,
        asyncMigrateOtherGroup: Boolean
    ): Boolean {
        return true
    }

    override fun autoRenewal(
        validExpiredDay: Int,
        projectConditionDTO: ProjectConditionDTO
    ): Boolean {
        return true
    }

    override fun migrateResourceAuthorization(projectCodes: List<String>): Boolean {
        return migrateResourceAuthorizationService.migrateResourceAuthorization(
            projectCodes = projectCodes
        )
    }

    override fun migrateAllResourceAuthorization(): Boolean {
        return migrateResourceAuthorizationService.migrateAllResourceAuthorization()
    }

    override fun fixResourceGroups(projectCodes: List<String>): Boolean = true

    override fun enablePipelineListPermissionControl(projectCodes: List<String>) = true
}
