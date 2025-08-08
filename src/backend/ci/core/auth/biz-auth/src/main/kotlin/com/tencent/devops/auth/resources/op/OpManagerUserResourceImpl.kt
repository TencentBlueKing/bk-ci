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

import com.tencent.devops.auth.api.manager.OpManagerUserResource
import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.auth.pojo.WhiteEntify
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.auth.pojo.enum.UrlType
import com.tencent.devops.auth.service.ManagerUserService
import com.tencent.devops.auth.service.UserPermissionService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class OpManagerUserResourceImpl @Autowired constructor(
    val mangerUserService: ManagerUserService,
    val userPermissionService: UserPermissionService
) : OpManagerUserResource {

    override fun createManagerUser(userId: String, managerUserDTO: ManagerUserDTO): Result<String> {
        return Result(mangerUserService.batchCreateManagerByUser(userId, managerUserDTO).toString())
    }

    override fun batchCreateManagerUser(
        userId: String,
        managerUserId: String,
        timeout: Int,
        managerIds: String
    ): Result<Boolean> {
        return Result(mangerUserService.batchCreateManager(
            userId = userId,
            managerId = managerIds,
            timeout = timeout,
            managerUser = managerUserId
        ))
    }

    override fun deleteManagerUser(userId: String, managerId: Int, deleteUser: String): Result<Boolean> {
        return Result(mangerUserService.deleteManagerUser(userId, managerId, deleteUser))
    }

    override fun batchDeleteManagerUser(userId: String, managerIds: String, deleteUsers: String): Result<Boolean> {
        return Result(mangerUserService.batchDelete(userId, managerIds, deleteUsers))
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

    override fun createWhiteUser(managerId: Int, userId: String): Result<Boolean> {
        return Result(mangerUserService.createWhiteUser(managerId, userId))
    }

    override fun listWhiteUser(managerId: Int): Result<List<WhiteEntify>?> {
        return Result(mangerUserService.listWhiteUser(managerId))
    }

    override fun deleteWhiteUser(ids: String): Result<Boolean> {
        return Result(mangerUserService.deleteWhiteUser(ids))
    }

    override fun getUrl(type: UrlType, managerId: Int): Result<String> {
        return Result(mangerUserService.getManagerUrl(managerId, type))
    }
}
