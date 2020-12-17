package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.OpManagerUserResource
import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.auth.service.ManagerUserService
import com.tencent.devops.auth.service.UserPermissionService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@RestResource
class OpManagerUserResourceImpl @Autowired constructor(
    val mangerUserService: ManagerUserService,
    val userPermissionService: UserPermissionService
) : OpManagerUserResource {

    override fun createManagerUser(userId: String, managerUserDTO: ManagerUserDTO): Result<String> {
        return Result(mangerUserService.createManagerUser(userId, managerUserDTO).toString())
    }

    override fun deleteManagerUser(userId: String, managerId: Int, deleteUser: String): Result<Boolean> {
        return Result(mangerUserService.deleteManagerUser(userId, managerId, deleteUser))
    }

    override fun managerAliveUserList(mangerId: Int): Result<List<ManagerUserEntity>?> {
        return Result(mangerUserService.aliveManagerListByManagerId(mangerId))
    }

    override fun managerHistoryUserList(mangerId: Int, page: Int?, size: Int?): Result<Page<ManagerUserEntity>?> {
        return Result(mangerUserService.timeoutManagerListByManagerId(mangerId, page, size))
    }

    override fun getManagerInfo(userId: String): Result<Map<String, UserPermissionInfo>?> {
        return Result(userPermissionService.getUserPermission(userId, false))
    }
}
