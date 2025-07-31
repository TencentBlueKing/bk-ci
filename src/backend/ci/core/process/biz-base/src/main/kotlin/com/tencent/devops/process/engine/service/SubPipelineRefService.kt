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

package com.tencent.devops.process.engine.service

import com.tencent.devops.model.process.tables.records.TPipelineSubRefRecord
import com.tencent.devops.process.engine.dao.SubPipelineRefDao
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class SubPipelineRefService @Autowired constructor(
    private val dslContext: DSLContext,
    private val subPipelineRefDao: SubPipelineRefDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRefService::class.java)
    }

    fun deleteAll(transaction: DSLContext? = null, projectId: String, pipelineId: String?) {
        val changeCount = subPipelineRefDao.deleteAll(
            dslContext = transaction ?: dslContext,
            pipelineId = pipelineId,
            projectId = projectId
        )
        logger.info("delete sub pipeline ref|$projectId|$pipelineId|$changeCount")
    }

    fun list(
        transaction: DSLContext? = null,
        projectId: String,
        pipelineId: String,
        subProjectId: String? = null,
        subPipelineId: String? = null
    ): Result<TPipelineSubRefRecord> {
        return subPipelineRefDao.list(
            dslContext = transaction ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            subProjectId = subProjectId,
            subPipelineId = subPipelineId
        )
    }

    fun batchAdd(transaction: DSLContext? = null, subPipelineRefList: List<SubPipelineRef>) {
        subPipelineRefDao.batchAdd(
            dslContext = transaction ?: dslContext,
            subPipelineRefList = subPipelineRefList
        )
    }

    fun batchDelete(transaction: DSLContext? = null, infos: Set<Triple<String, String, String>>) {
        subPipelineRefDao.batchDelete(
            dslContext = transaction ?: dslContext,
            infos = infos
        )
    }

    fun delete(projectId: String, pipelineId: String, taskId: String) {
        logger.info("delete sub pipeline ref|projectId[$projectId]|pipelineId[$pipelineId]|taskId[$taskId]")
        subPipelineRefDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId
        )
    }

    fun cleanUpInvalidRefs(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        existsTaskIds: Set<String>,
        invalidTaskIds: Set<String>
    ) {
        val taskIdRecords = subPipelineRefDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ).map { it.taskId }
        val needDelInfos = taskIdRecords.filter { !existsTaskIds.contains(it) }.map {
            Triple(projectId, pipelineId, it)
        }.toMutableSet()
        // 填充需要同时移除的无效引用信息
        needDelInfos.addAll(invalidTaskIds.map { Triple(projectId, pipelineId, it) })
        batchDelete(
            transaction = dslContext,
            infos = needDelInfos
        )
    }

    /**
     * 是否存在调用链路
     */
    fun exists(
        projectId: String,
        pipelineId: String,
        subProjectId: String,
        subPipelineId: String
    ) = list(
        projectId = projectId,
        pipelineId = pipelineId,
        subProjectId = subProjectId,
        subPipelineId = subPipelineId
    ).isNotEmpty
}
