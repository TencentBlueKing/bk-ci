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

package com.tencent.devops.process.engine.service

import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineRepositoryVersionService constructor(
    private val dslContext: DSLContext,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao
) {

    fun deletePipelineVer(projectId: String, pipelineId: String, version: Int) {
        dslContext.transaction { t ->
            val transactionContext = DSL.using(t)
            pipelineResVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
            pipelineSettingVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
        }
    }

    fun listPipelineVersion(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Pair<Int, List<PipelineInfo>> {
        if (pipelineInfo == null) {
            return Pair(0, emptyList())
        }

        val count = pipelineResVersionDao.count(dslContext, projectId, pipelineId)
        val result = pipelineResVersionDao.listPipelineVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        val list = mutableListOf<PipelineInfo>()

        result.forEach {
            list.add(pipelineInfo.copy(
                createTime = it.createTime,
                creator = it.creator,
                version = it.version,
                versionName = it.versionName)
            )
        }
        return count to list
    }

    companion object {
        private const val MAX_LEN_FOR_NAME = 255
        private val logger = LoggerFactory.getLogger(PipelineRepositoryVersionService::class.java)
    }
}
