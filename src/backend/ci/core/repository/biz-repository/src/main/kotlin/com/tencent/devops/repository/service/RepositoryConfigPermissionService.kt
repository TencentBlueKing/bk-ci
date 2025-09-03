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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthPlatformApi
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.repository.dao.RepositoryConfigDeptDao
import com.tencent.devops.repository.pojo.RepositoryConfigDept
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

/**
 * 代码库源权限抽象类
 */
abstract class RepositoryConfigPermissionService @Autowired constructor(
    open val dslContext: DSLContext,
    open val authPlatformApi: AuthPlatformApi,
    open val repositoryConfigDeptDao: RepositoryConfigDeptDao,
    open val client: Client
) {

    /**
     * 获取用户组织信息
     */
    abstract fun getUserDeptList(userId: String): List<Int>

    /**
     * 查询用户项目下支持的代码源
     */
    fun listScmCode(userId: String, scmCodes: List<String>): List<String> {
        val userDeptList = getUserDeptList(userId)
        if (userDeptList.isEmpty()) return scmCodes
        val result = mutableListOf<String>()
        scmCodes.forEach { scmCode ->
            val deptList = repositoryConfigDeptDao.list(
                dslContext = dslContext,
                scmCode = scmCode,
                limit = MAX_DEPT_COUNT,
                offset = 0
            ).map { it.deptId }
            deptList.forEach deptEach@{ deptId ->
                if (!result.contains(scmCode) && validateDeptId(deptId, userDeptList)) {
                    result.add(scmCode)
                }
            }
        }
        return result
    }

    /**
     * 校验管理员权限
     */
    fun validateAdminPerm(userId: String) {
        if (!authPlatformApi.validateUserPlatformPermission(userId)) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.ERROR_USER_NO_PLATFORM_ADMIN_PERMISSION
                )
            )
        }
    }

    fun create(
        scmCode: String,
        deptList: List<RepositoryConfigDept>,
        userId: String
    ) {
        repositoryConfigDeptDao.create(
            dslContext = dslContext,
            scmCode = scmCode,
            deptList = deptList,
            userId = userId
        )
    }

    fun delete(
        scmCode: String,
        deptList: Set<Int>
    ) = repositoryConfigDeptDao.delete(
        dslContext = dslContext,
        scmCode = scmCode,
        deptList = deptList
    )

    fun list(
        scmCode: String,
        limit: Int,
        offset: Int
    ) = repositoryConfigDeptDao.list(
        dslContext = dslContext,
        scmCode = scmCode,
        limit = limit,
        offset = offset
    ).toList().map {
        RepositoryConfigDept(
            deptId = it.deptId,
            deptName = it.deptName
        )
    }

    private fun validateDeptId(
        deptId: Int,
        userDeptIdList: List<Int>
    ): Boolean {
        return if (deptId == 0 || userDeptIdList.contains(deptId)) {
            true // 用户在组件的可见范围内
        } else {
            // 判断该可见范围是否设置了全公司可见
            client.get(ServiceProjectOrganizationResource::class).getParentDeptInfos(
                deptId = deptId.toString(),
                level = 1
            ).data?.let {
                // 没有上级机构说明设置的可见范围是全公司
                return it.isEmpty()
            }
            false
        }
    }

    companion object {
        const val MAX_DEPT_COUNT = 100
    }
}
