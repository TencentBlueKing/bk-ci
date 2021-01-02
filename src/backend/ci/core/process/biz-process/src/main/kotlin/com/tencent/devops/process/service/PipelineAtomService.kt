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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.pojo.PipelineAtomReplaceRequest
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
        pipelineAtomReplaceRequest: PipelineAtomReplaceRequest
    ): Result<Boolean> {
        logger.info("createReplaceAtomInfo [$userId|$projectId|$pipelineAtomReplaceRequest]")
        val baseId = UUIDUtil.generate()
        val fromAtomCode = pipelineAtomReplaceRequest.fromAtomCode
        val toAtomCode = pipelineAtomReplaceRequest.toAtomCode
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineAtomReplaceBaseDao.createAtomReplaceBase(
                dslContext = context,
                baseId = baseId,
                projectId = projectId,
                pipelineIdList = pipelineAtomReplaceRequest.pipelineIdList,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                userId = userId
            )
            pipelineAtomReplaceItemDao.createAtomReplaceItem(
                dslContext = context,
                baseId = baseId,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                replaceItemList = pipelineAtomReplaceRequest.replaceItemList,
                userId = userId
            )
        }
        return Result(true)
    }
}
