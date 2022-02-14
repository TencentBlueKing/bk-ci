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

package com.tencent.devops.misc.service.project

import com.tencent.devops.misc.dao.project.ProjectMiscDao
import com.tencent.devops.misc.dao.project.TxProjectMiscDao
import com.tencent.devops.misc.pojo.ProjectShardingInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxProjectMiscService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectMiscDao: ProjectMiscDao,
    private val txProjectMiscDao: TxProjectMiscDao
) {

    fun getMaxId(
        projectIdList: List<String>? = null
    ): Long? {
        return projectMiscDao.getMaxId(dslContext, projectIdList)
    }

    fun getProjectShardingInfoList(
        projectIdList: List<String>? = null,
        minId: Long? = null,
        maxId: Long? = null,
        channelCodeList: List<String>? = null,
        dsName: String? = null
    ): List<ProjectShardingInfo>? {
        val projectShardingInfoRecords = txProjectMiscDao.getProjectInfoList(
            dslContext = dslContext,
            projectIdList = projectIdList,
            minId = minId,
            maxId = maxId,
            channelCodeList = channelCodeList,
            dsName = dsName
        )
        return if (projectShardingInfoRecords == null) {
            null
        } else {
            val projectShardingInfoList = mutableListOf<ProjectShardingInfo>()
            projectShardingInfoRecords.forEach { projectShardingInfoRecord ->
                projectShardingInfoList.add(
                    ProjectShardingInfo(
                        id = projectShardingInfoRecord["ID"] as Long,
                        projectId = projectShardingInfoRecord["ENGLISH_NAME"] as String,
                        channel = projectShardingInfoRecord["CHANNEL"] as String,
                        routingRule = projectShardingInfoRecord["ROUTING_RULE"] as? String
                    )
                )
            }
            projectShardingInfoList
        }
    }
}
