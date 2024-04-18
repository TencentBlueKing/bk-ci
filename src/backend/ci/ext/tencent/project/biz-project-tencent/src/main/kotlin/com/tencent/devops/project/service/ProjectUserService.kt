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

package com.tencent.devops.project.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectSeniorUserDao
import com.tencent.devops.project.dao.ProjectUserDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.SeniorUserDTO
import com.tencent.devops.project.pojo.user.UserDeptDetail
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class ProjectUserService @Autowired constructor(
    val dslContext: DSLContext,
    val userDao: UserDao,
    val projectUserDao: ProjectUserDao,
    val seniorUserDao: ProjectSeniorUserDao,
    val projectDao: ProjectDao
) {
    private val seniorUserCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(7, TimeUnit.DAYS)
        .build<String, String>()

    fun getUserDept(userId: String): UserDeptDetail? {
        val userRecord = userDao.get(dslContext, userId) ?: return null
        return packagingBean(userRecord)
    }

    fun getPublicAccount(userId: String): UserDeptDetail? {
        val userRecord = userDao.getPublicType(dslContext, userId) ?: return null
        return packagingBean(userRecord)
    }

    fun packagingBean(userRecord: TUserRecord): UserDeptDetail {
        return UserDeptDetail(
            bgName = userRecord!!.bgName,
            bgId = userRecord!!.bgId?.toString() ?: "",
            centerName = userRecord.centerName,
            centerId = userRecord!!.centerId?.toString() ?: "",
            deptName = userRecord.deptName,
            deptId = userRecord.deptId?.toString() ?: "",
            groupName = userRecord.groupName ?: "",
            groupId = userRecord.groypId?.toString() ?: "",
            businessLineId = userRecord.businessLineId?.toString(),
            businessLineName = userRecord.businessLineName
        )
    }

    fun listUser(limit: Int, offset: Int): List<UserDeptDetail>? {
        val limitByMax = if (limit > 1000) {
            1000
        } else {
            limit
        }

        return projectUserDao.list(
            dslContext = dslContext,
            limit = limitByMax,
            offset = offset
        )?.map {
            UserDeptDetail(
                bgName = it!!.bgName,
                bgId = it.bgId?.toString() ?: "",
                centerName = it.centerName ?: "",
                centerId = it.centerId?.toString() ?: "",
                deptName = it.deptName ?: "",
                deptId = it.deptId?.toString() ?: "",
                groupName = it.groupName ?: "",
                groupId = it.groypId?.toString() ?: "",
                userId = it.userId,
                name = it.name,
                businessLineId = it.businessLineId?.toString(),
                businessLineName = it.businessLineName
            )
        }
    }

    fun getRemoteDevAdmin(projectIds: Set<String>): Map<String, Set<String>?> {
        // 获取项目的云研发管理员
        val projects = projectDao.listByCodes(dslContext, projectIds, enabled = true)
        val res = mutableMapOf<String, MutableSet<String>>()
        projects.forEach { project ->
            val projectProperties = project.properties?.let {
                JsonUtil.toOrNull(project.properties.toString(), ProjectProperties::class.java)
            } ?: return@forEach

            if (projectProperties.remotedev != true) {
                return@forEach
            }

            res[project.englishName] = projectProperties.remotedevManager?.split(";")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet() ?: mutableSetOf()
        }

        return res
    }

    fun creatSeniorUser(seniorUserList: List<SeniorUserDTO>): Boolean {
        seniorUserList.forEach {
            seniorUserDao.create(
                dslContext = dslContext,
                seniorUserDTO = it
            )
        }
        return true
    }

    fun deleteSeniorUser(userId: String): Boolean {
        seniorUserDao.delete(
            dslContext = dslContext,
            userId = userId
        )
        return true
    }

    fun isSeniorUser(userId: String): Boolean {
        if (seniorUserCache.asMap().isEmpty()) {
            logger.info("refresh senior user cache")
            seniorUserDao.list(
                dslContext = dslContext
            ).forEach {
                seniorUserCache.put(it.userId, it.name)
            }
        }
        return seniorUserCache.getIfPresent(userId) != null
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectUserService::class.java)
    }
}
