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

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.repository.dao.RepositoryConfigVisibilityDao
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

/**
 * 代码库源权限抽象类
 */
abstract class RepositoryConfigVisibilityService @Autowired constructor(
    open val dslContext: DSLContext,
    open val repositoryConfigVisibilityDao: RepositoryConfigVisibilityDao,
    open val client: Client
) {
    // 代码源可见性信息
    private val repoVisibilityCache = CacheHelper.createCache<String, List<Int>>(
        duration = 5,
        maxSize = 20
    )

    // 用户组织架构缓存
    private val userOrgCache = CacheHelper.createCache<String, List<Int>>(
        duration = 5,
        maxSize = 5000
    )

    /**
     * 获取用户组织信息
     */
    abstract fun getUserDeptList(userId: String): List<Int>

    /**
     * 查询用户项目下支持的代码源
     */
    fun listScmCode(userId: String, scmCodes: List<String>): List<String> {
        val userDeptList = userOrgCache.get(userId) {
            getUserDeptList(userId)
        }
        if (userDeptList.isEmpty()) return scmCodes
        val result = mutableListOf<String>()
        scmCodes.forEach scmEach@{ scmCode ->
            val deptList = repoVisibilityCache.get(scmCode) {
                repositoryConfigVisibilityDao.list(
                    dslContext = dslContext,
                    scmCode = scmCode,
                    limit = MAX_DEPT_COUNT,
                    offset = 0
                ).map { it.deptId }
            }
            deptList.forEach deptEach@{ deptId ->
                if (validateDeptId(deptId, userDeptList)) {
                    result.add(scmCode)
                    return@scmEach
                }
            }
        }
        return result
    }

    fun createDept(
        scmCode: String,
        deptList: List<RepositoryConfigVisibility>,
        userId: String
    ) {
        repositoryConfigVisibilityDao.create(
            dslContext = dslContext,
            scmCode = scmCode,
            deptList = deptList,
            userId = userId
        )
    }

    fun deleteDept(
        scmCode: String,
        deptList: Set<Int>
    ) = repositoryConfigVisibilityDao.delete(
        dslContext = dslContext,
        scmCode = scmCode,
        deptList = deptList
    )

    fun listDept(
        scmCode: String,
        limit: Int,
        offset: Int
    ): List<RepositoryConfigVisibility> = repositoryConfigVisibilityDao.list(
        dslContext = dslContext,
        scmCode = scmCode,
        limit = limit,
        offset = offset
    ).map {
        RepositoryConfigVisibility(
            deptId = it.deptId,
            deptName = it.deptName
        )
    }

    fun countDept(
        scmCode: String
    ) = repositoryConfigVisibilityDao.count(
        dslContext = dslContext,
        scmCode = scmCode
    )

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
        const val MAX_DEPT_COUNT = 1000
    }
}