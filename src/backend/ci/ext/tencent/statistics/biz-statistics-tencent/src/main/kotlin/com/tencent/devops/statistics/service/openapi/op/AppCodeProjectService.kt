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
import com.tencent.devops.model.openapi.tables.records.TAppCodeProjectRecord
import com.tencent.devops.statistics.dao.openapi.AppCodeProjectDao
import com.tencent.devops.statistics.pojo.openapi.AppCodeProjectResponse
import org.jooq.DSLContext
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2020/3/17
 * @Version 1.0
 */
@Service
class AppCodeProjectService(
    private val dslContext: DSLContext,
    private val appCodeProjectDao: AppCodeProjectDao
) {
    fun addProject(userName: String, appCode: String, projectId: String): Boolean {
        val projectIdList = projectId.split(" |;".toRegex())
        var result = false
        projectIdList.forEach {
            val projectIdTmp = it.trim()
            if (projectIdTmp.isNotBlank()) {
                result = appCodeProjectDao.add(dslContext, userName, appCode, it)
                if (!result) {
                    return result
                }
            }
        }
        return result
    }

    fun deleteProject(userName: String, appCode: String, projectId: String): Boolean {
        return appCodeProjectDao.delete(dslContext, appCode, projectId)
    }

    fun getProject(userName: String, appCode: String, projectId: String): AppCodeProjectResponse? {
        val record = appCodeProjectDao.get(dslContext, appCode, projectId)
        return if (record == null) {
            null
        } else {
            tranform(record)
        }
    }

    fun listProject(userName: String): List<AppCodeProjectResponse> {
        val records = appCodeProjectDao.list(dslContext)
        val resultList = mutableListOf<AppCodeProjectResponse>()
        records.forEach {
            resultList.add(tranform(it))
        }
        return resultList
    }

    fun listProjectByAppCode(appCode: String): List<AppCodeProjectResponse> {
        val records = appCodeProjectDao.listByAppCode(dslContext, appCode)
        val resultList = mutableListOf<AppCodeProjectResponse>()
        records.forEach {
            resultList.add(tranform(it))
        }
        return resultList
    }

    private fun tranform(record: TAppCodeProjectRecord): AppCodeProjectResponse {
        return AppCodeProjectResponse(
            id = record.id,
            appCode = record.appCode,
            projectId = record.projectId,
            creator = record.creator,
            createTime = record.createTime.timestampmilli()
        )
    }
}
