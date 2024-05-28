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

package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.migrate.OpAuthMigrateResource
import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAuthMigrateResourceImpl @Autowired constructor(
    private val permissionMigrateService: PermissionMigrateService
) : OpAuthMigrateResource {

    override fun v3ToRbacAuth(projectCodes: List<String>): Result<Boolean> {
        return Result(permissionMigrateService.v3ToRbacAuth(projectCodes = projectCodes))
    }

    override fun v0ToRbacAuth(projectCodes: List<String>): Result<Boolean> {
        return Result(permissionMigrateService.v0ToRbacAuth(projectCodes = projectCodes))
    }

    override fun allToRbacAuth(): Result<Boolean> {
        return Result(permissionMigrateService.allToRbacAuth())
    }

    override fun toRbacAuthByCondition(projectConditionDTO: ProjectConditionDTO): Result<Boolean> {
        return Result(
            permissionMigrateService.toRbacAuthByCondition(
                projectConditionDTO = projectConditionDTO
            )
        )
    }

    override fun compareResult(projectCode: String): Result<Boolean> {
        return Result(permissionMigrateService.compareResult(projectCode = projectCode))
    }

    override fun migrateSpecificResource(migrateResourceDTO: MigrateResourceDTO): Result<Boolean> {
        return Result(
            permissionMigrateService.migrateSpecificResource(migrateResourceDTO = migrateResourceDTO)
        )
    }

    override fun migrateSpecificResourceOfAllProject(migrateResourceDTO: MigrateResourceDTO): Result<Boolean> {
        return Result(
            permissionMigrateService.migrateSpecificResourceOfAllProject(migrateResourceDTO = migrateResourceDTO)
        )
    }

    override fun grantGroupAdditionalAuthorization(projectCodes: List<String>): Result<Boolean> {
        return Result(permissionMigrateService.grantGroupAdditionalAuthorization(projectCodes = projectCodes))
    }

    override fun handoverAllPermissions(permissionHandoverDTO: PermissionHandoverDTO): Result<Boolean> {
        return Result(permissionMigrateService.handoverAllPermissions(permissionHandoverDTO = permissionHandoverDTO))
    }

    override fun handoverPermissions(permissionHandoverDTO: PermissionHandoverDTO): Result<Boolean> {
        return Result(permissionMigrateService.handoverPermissions(permissionHandoverDTO = permissionHandoverDTO))
    }

    override fun migrateMonitorResource(projectCodes: List<String>): Result<Boolean> {
        return Result(permissionMigrateService.migrateMonitorResource(projectCodes = projectCodes))
    }

    override fun autoRenewal(projectConditionDTO: ProjectConditionDTO): Result<Boolean> {
        permissionMigrateService.autoRenewal(projectConditionDTO)
        return Result(true)
    }
}
