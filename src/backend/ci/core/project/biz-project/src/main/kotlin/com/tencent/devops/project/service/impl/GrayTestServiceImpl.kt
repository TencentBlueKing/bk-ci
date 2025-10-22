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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.service.Profile
import com.tencent.devops.project.dao.GrayTestDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.pojo.service.GrayTestInfo
import com.tencent.devops.project.pojo.service.GrayTestListInfo
import com.tencent.devops.project.service.GrayTestService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GrayTestServiceImpl @Autowired constructor(
    private val profile: Profile,
    private val dslContext: DSLContext,
    private val grayTestDao: GrayTestDao,
    private val serviceDao: ServiceDao
) : GrayTestService {
    override fun create(userId: String, grayTestInfo: GrayTestInfo): GrayTestInfo {
        return grayTestDao.create(dslContext, userId, grayTestInfo.server_id, grayTestInfo.status)
    }

    override fun update(userId: String, id: Long, grayTestInfo: GrayTestInfo) {
        grayTestDao.update(dslContext, userId, grayTestInfo.server_id, grayTestInfo.status, id)
    }

    override fun delete(id: Long) {
        grayTestDao.delete(dslContext, id)
    }

    override fun get(id: Long): GrayTestInfo {
        return grayTestDao.get(dslContext, id)
    }

    override fun listByUser(userId: String): List<GrayTestInfo> {
        return grayTestDao.listByUser(dslContext, userId)
    }

    override fun listByCondition(
        userNameList: List<String>,
        serviceIdList: List<String>,
        statusList: List<String>,
        pageSize: Int,
        pageNum: Int
    ): List<GrayTestListInfo> {
        val notNullUsers = userNameList.filterNot { it == "" }
        val notNullIds = serviceIdList.filterNot { it == "" }
        val notNullStatus = statusList.filterNot { it == "" }

        val grayList =
            grayTestDao.listByCondition(dslContext, notNullUsers, notNullIds, notNullStatus, pageSize, pageNum)
        val totalRecord = grayTestDao.getSum(dslContext)
        val serviceList = serviceDao.getServiceList(
            dslContext = dslContext,
            clusterType = if (profile.isDevx()) "devx" else ""
        )
        return grayList.map {
            val server = serviceList.filter { it2 -> it.server_id == it2.id }[0]
            GrayTestListInfo(it.id, it.server_id, server.name, it.userName, it.status, totalRecord)
        }
    }

    override fun listAllUsers(): Map<String, List<Any>> {
        val allUsers = grayTestDao.listAllUsers(dslContext)
        val allService = grayTestDao.listAllService(dslContext)
        val map = HashMap<String, List<Any>>()
        map["users"] = allUsers
        map["services"] = allService
        return map
    }
}
