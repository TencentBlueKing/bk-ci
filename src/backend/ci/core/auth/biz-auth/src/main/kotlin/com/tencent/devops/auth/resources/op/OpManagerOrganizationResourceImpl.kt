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
 */

package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.manager.OpManagerOrganizationResource
import com.tencent.devops.auth.pojo.ManageOrganizationEntity
import com.tencent.devops.auth.pojo.dto.ManageOrganizationDTO
import com.tencent.devops.auth.service.ManagerOrganizationService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpManagerOrganizationResourceImpl @Autowired constructor(
    val managerOrganizationService: ManagerOrganizationService
) : OpManagerOrganizationResource {

    override fun createManagerOrganization(
        userId: String,
        managerOrganization: ManageOrganizationDTO
    ): Result<String> {
        return Result(managerOrganizationService.createManagerOrganization(userId, managerOrganization).toString())
    }

    override fun updateManagerOrganization(
        userId: String,
        managerId: Int,
        managerOrganization: ManageOrganizationDTO
    ): Result<Boolean> {
        return Result(managerOrganizationService.updateManagerOrganization(userId, managerOrganization, managerId))
    }

    override fun deleteManagerOrganization(userId: String, managerId: Int): Result<Boolean> {
        return Result(managerOrganizationService.deleteManagerOrganization(userId, managerId))
    }

    override fun getManagerOrganization(userId: String, managerId: Int): Result<ManageOrganizationEntity?> {
        return Result(managerOrganizationService.getManagerOrganization(managerId))
    }

    override fun listManagerOrganization(userId: String): Result<List<ManageOrganizationEntity>?> {
        return Result(managerOrganizationService.listOrganization())
    }
}
