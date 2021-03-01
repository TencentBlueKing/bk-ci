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

import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectUserRefreshService @Autowired constructor(
    val tofService: TOFService,
    val projectUserService: ProjectUserService,
    val userDao: UserDao,
    val dslContext: DSLContext
) {
    fun refreshUser(userId: String): UserDeptDetail? {
        val userRecord = projectUserService.getUserDept(userId)
        return if (userRecord == null) {
            logger.info("user is empty, add userInfo from tof")
            createUser(userId)
        } else {
            synUserInfo(userRecord, userId)
        }
    }

    // 添加用户
    fun createUser(userId: String): UserDeptDetail {
        // user表不存在，直接同步 数据源直接获取tof数据
        val tofDeptInfo = tofService.getDeptFromTof(null, userId, "", false)
        val staffInfo = tofService.getStaffInfo(userId)
        userDao.create(
                dslContext = dslContext,
                groupId = tofDeptInfo.groupId.toInt(),
                groupName = tofDeptInfo.groupName,
                bgId = tofDeptInfo.bgId.toInt(),
                bgName = tofDeptInfo.bgName,
                centerId = tofDeptInfo.deptId.toInt(),
                centerName = tofDeptInfo.deptName,
                deptId = tofDeptInfo.deptId.toInt(),
                deptName = tofDeptInfo.deptName,
                name = staffInfo.ChineseName,
                userId = userId
        )
        return tofDeptInfo
    }

    // 同步用户信息
    fun synUserInfo(userInfo: UserDeptDetail, userId: String): UserDeptDetail? {
        val staffInfo = tofService.getStaffInfo(userId)
        if (userInfo!!.groupId != staffInfo.GroupId) {
            logger.info("user info diff, bk:${userInfo.groupId}, tof :${staffInfo.GroupId}")
            // 组织信息不一致，刷新当前用户数据。 以tof数据为准, 数据源直接获取tof数据
            val tofDeptInfo = tofService.getDeptFromTof(null, userId, "", false)
            userDao.update(
                    userId = userId,
                    groupId = tofDeptInfo.groupId.toInt(),
                    groupName = tofDeptInfo.groupName,
                    bgId = tofDeptInfo.bgId.toInt(),
                    bgName = tofDeptInfo.bgName,
                    centerId = tofDeptInfo.deptId.toInt(),
                    centerName = tofDeptInfo.deptName,
                    deptId = tofDeptInfo.deptId.toInt(),
                    deptName = tofDeptInfo.deptName,
                    dslContext = dslContext,
                    name = staffInfo.ChineseName
            )
            return tofDeptInfo
        }
        return null
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
