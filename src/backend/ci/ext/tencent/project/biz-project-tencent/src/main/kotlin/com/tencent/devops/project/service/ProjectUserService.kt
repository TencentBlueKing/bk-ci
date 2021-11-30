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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.dao.ProjectUserDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.UserInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectUserService @Autowired constructor(
    val dslContext: DSLContext,
    val userDao: UserDao,
    val projectUserDao: ProjectUserDao,
    val tofService: TOFService
) {
    fun getUserDept(userId: String): UserDeptDetail? {
        val userRecord = userDao.get(dslContext, userId) ?: return null
        return UserDeptDetail(
                bgName = userRecord!!.bgName,
                bgId = userRecord!!.bgId?.toString() ?: "",
                centerName = userRecord.centerName,
                centerId = userRecord!!.centerId?.toString() ?: "",
                deptName = userRecord.deptName,
                deptId = userRecord.deptId?.toString() ?: "",
                groupName = userRecord.groupName,
                groupId = userRecord.groypId?.toString() ?: ""
        )
    }

    fun listUser(limit: Int, offset: Int): List<TUserRecord>? {
        val limitByMax = if (limit > 1000) {
            1000
        } else {
            limit
        }

        return projectUserDao.list(
            dslContext = dslContext,
            limit = limitByMax,
            offset = offset
        )
    }

    fun createPublicAccount(userInfo: UserInfo): Boolean {
        val userMessage = userDao.get(dslContext, userInfo.userId)
        if (userMessage != null) {
            logger.warn("createPublicAccount ${userInfo.userId} ${userInfo.name} is exist")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(userInfo.userId)
            )
        }
        try {
            // 校验是否在rtx用户, 若为rtx用户不符合公共账号的判断逻辑
            tofService.getUserDeptDetail(userInfo.userId)
            logger.warn("createPublicAccount ${userInfo.userId} is not public account")
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                params = arrayOf(userInfo.userId)
            )
        } catch (e: OperationException) {
            logger.info("createPublicAccount ${userInfo.userId} is public account")
        }

        userDao.create(
            dslContext = dslContext,
            userId = userInfo.userId,
            name = userInfo.userId,
            bgId = userInfo.bgId,
            bgName = userInfo.bgName,
            deptId = userInfo.deptId ?: 0,
            deptName = userInfo.deptName ?: "",
            centerId = userInfo.centerId ?: 0,
            centerName = userInfo.centerName ?: "",
            groupId = userInfo.groupId ?: 0,
            groupName = userInfo.groupName ?: ""
        )
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectUserService::class.java)
    }
}
