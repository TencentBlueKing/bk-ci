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
package com.tencent.devops.statistics.service.openapi.op

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.openapi.tables.records.TAppCodeGroupRecord
import com.tencent.devops.statistics.dao.openapi.AppCodeGroupDao
import com.tencent.devops.statistics.pojo.openapi.AppCodeGroup
import com.tencent.devops.statistics.pojo.openapi.AppCodeGroupResponse
import org.jooq.DSLContext
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2020/3/17
 * @Version 1.0
 */
@Service
class AppCodeGroupService(
    private val dslContext: DSLContext,
    private val appCodeGroupDao: AppCodeGroupDao
) {
    fun setGroup(userName: String, appCode: String, appCodeGroup: AppCodeGroup): Boolean {
        return appCodeGroupDao.set(dslContext, userName, appCode, appCodeGroup)
    }

    fun getGroup(appCode: String): AppCodeGroupResponse? {
        val record = appCodeGroupDao.get(dslContext, appCode)
        return if (record == null) {
            null
        } else {
            tranform(record)
        }
    }

    fun listGroup(userName: String): List<AppCodeGroupResponse> {
        val records = appCodeGroupDao.list(dslContext)
        val resultList = mutableListOf<AppCodeGroupResponse>()
        records.forEach {
            resultList.add(tranform(it))
        }
        return resultList
    }

    fun deleteGroup(userName: String, appCode: String): Boolean {
        return appCodeGroupDao.delete(dslContext, appCode)
    }

    private fun tranform(record: TAppCodeGroupRecord): AppCodeGroupResponse {
        return AppCodeGroupResponse(
            id = record.id,
            appCode = record.appCode,
            bgId = record.bgId,
            bgName = record.bgName,
            deptId = record.deptId,
            deptName = record.deptName,
            centerId = record.centerId,
            centerName = record.centerName,
            creator = record.creator,
            createTime = record.createTime.timestampmilli(),
            updater = record.updater,
            updateTime = record.updateTime.timestampmilli()
        )
    }
}
