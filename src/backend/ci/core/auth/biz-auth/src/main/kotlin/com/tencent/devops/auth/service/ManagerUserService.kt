package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.dao.ManagerUserHistoryDao
import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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

@Service
class ManagerUserService @Autowired constructor(
    val dslContext: DSLContext,
    val mangerUserDao: ManagerUserDao,
    val mangerUserHistoryDao: ManagerUserHistoryDao
) {

    fun createManagerUser(userId: String, mangerUser: ManagerUserDTO) : Int {
        val managerInfo = ManagerUserEntity(
            createUser = userId,
            startTime = System.currentTimeMillis(),
            timeoutTime = System.currentTimeMillis() + DateTimeUtil.minuteToSecond(mangerUser.timeout) * 1000,
            userId = mangerUser.userId
        )
        val id = mangerUserDao.create(dslContext, managerInfo)
        mangerUserHistoryDao.create(dslContext, managerInfo)
        return id
    }

    fun aliveManagerListByManagerId(managerId: Int) : List<ManagerUserEntity>? {

        val userRecords = mangerUserDao.list(dslContext, managerId) ?: return null

        val managerList = mutableListOf<ManagerUserEntity>()

        userRecords.forEach {
            val manager = ManagerUserEntity(
                userId = it.userId,
                createUser = it.createUser,
                timeoutTime = DateTimeUtil.toDateTime(it.endTime).toLong(),
                startTime = DateTimeUtil.toDateTime(it.startTime).toLong()
            )
            managerList.add(manager)
        }
        return managerList
    }

    fun timeoutManagerListByManagerId(managerId: Int, page: Int, pageSize: Int): Page<ManagerUserEntity>? {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val userRecords = mangerUserHistoryDao.list(dslContext, managerId, sqlLimit.limit, sqlLimit.offset) ?: return null
        val count = mangerUserHistoryDao.count(dslContext, managerId)
        val managerList = mutableListOf<ManagerUserEntity>()

        userRecords.forEach {
            val manager = ManagerUserEntity(
                userId = it.userId,
                createUser = it.createUser,
                timeoutTime = DateTimeUtil.toDateTime(it.endTime).toLong(),
                startTime = DateTimeUtil.toDateTime(it.startTime).toLong()
            )
            managerList.add(manager)
        }
        return Page(
            count = count.toLong(),
            page = page,
            pageSize = pageSize,
            records = managerList
        )
    }
}
