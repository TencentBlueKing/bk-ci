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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.enums.TaskStatusEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.store.pojo.atom.AtomReplaceRequest
import com.tencent.devops.store.pojo.atom.AtomReplaceRollBack
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomService::class.java)
    }

    fun createReplaceAtomInfo(
        userId: String,
        projectId: String?,
        atomReplaceRequest: AtomReplaceRequest
    ): Result<String> {
        logger.info("createReplaceAtomInfo [$userId|$projectId|$atomReplaceRequest]")
        val baseId = UUIDUtil.generate()
        val fromAtomCode = atomReplaceRequest.fromAtomCode
        val toAtomCode = atomReplaceRequest.toAtomCode
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineAtomReplaceBaseDao.createAtomReplaceBase(
                dslContext = context,
                baseId = baseId,
                projectId = projectId,
                pipelineIdList = atomReplaceRequest.pipelineIdList,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                userId = userId
            )
            pipelineAtomReplaceItemDao.createAtomReplaceItem(
                dslContext = context,
                baseId = baseId,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                versionInfoList = atomReplaceRequest.versionInfoList,
                userId = userId
            )
        }
        return Result(baseId)
    }

    fun atomReplaceRollBack(
        userId: String,
        atomReplaceRollBack: AtomReplaceRollBack
    ): Result<Boolean> {
        logger.info("atomReplaceRollBack [$userId|$atomReplaceRollBack]")
        val baseId = atomReplaceRollBack.baseId
        val itemId = atomReplaceRollBack.itemId
        // 将任务状态更新为”待回滚“状态
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                dslContext = context,
                baseId = baseId,
                status = TaskStatusEnum.PENDING_ROLLBACK.name,
                userId = userId
            )
            if (itemId != null) {
                pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                    dslContext = context,
                    itemId = itemId,
                    status = TaskStatusEnum.PENDING_ROLLBACK.name,
                    userId = userId
                )
            } else {
                pipelineAtomReplaceItemDao.updateAtomReplaceItemByBaseId(
                    dslContext = context,
                    baseId = baseId,
                    status = TaskStatusEnum.PENDING_ROLLBACK.name,
                    userId = userId
                )
            }
        }
        return Result(true)
    }
}
