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

package com.tencent.devops.misc.service.project

import com.tencent.devops.misc.dao.project.ProjectMiscDao
import com.tencent.devops.misc.pojo.project.ProjectInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectMiscService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectMiscDao: ProjectMiscDao
) {
    fun getMinId(
        projectIdList: List<String>? = null
    ): Long? {
        return projectMiscDao.getMinId(dslContext, projectIdList)
    }

    fun getMaxId(
        projectIdList: List<String>? = null
    ): Long? {
        return projectMiscDao.getMaxId(dslContext, projectIdList)
    }

    fun getProjectInfoList(
        projectIdList: List<String>? = null,
        minId: Long? = null,
        maxId: Long? = null,
        channelCodeList: List<String>? = null
    ): List<ProjectInfo>? {
        val projectInfoRecords = projectMiscDao.getProjectInfoList(
            dslContext = dslContext,
            projectIdList = projectIdList,
            minId = minId,
            maxId = maxId,
            channelCodeList = channelCodeList
        )
        return if (projectInfoRecords == null) {
            null
        } else {
            val projectInfoList = mutableListOf<ProjectInfo>()
            projectInfoRecords.forEach { projectInfoRecord ->
                projectInfoList.add(
                    ProjectInfo(
                        id = projectInfoRecord["ID"] as Long,
                        projectId = projectInfoRecord["ENGLISH_NAME"] as String,
                        channel = projectInfoRecord["CHANNEL"] as String
                    )
                )
            }
            projectInfoList
        }
    }
}
