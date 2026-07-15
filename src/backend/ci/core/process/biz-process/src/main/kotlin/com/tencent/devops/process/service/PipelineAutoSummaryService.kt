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

package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线AI摘要服务
 * 提供流水线AI自动摘要的更新操作，包含流水线锁和版本校验
 */
@Service
class PipelineAutoSummaryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation
) {

    /**
     * 更新流水线AI自动摘要
     * 使用流水线锁保证并发安全，并校验版本号（乐观锁），若版本不匹配则放弃更新
     *
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param autoSummary AI生成的摘要内容
     * @param version 流水线版本号（用于校验，避免过期摘要覆盖新版本）
     * @return 是否更新成功
     */
    fun updateAutoSummary(
        projectId: String,
        pipelineId: String,
        autoSummary: String,
        version: Int
    ): Boolean {
        val lock = PipelineModelLock(redisOperation, pipelineId)
        try {
            lock.lock()
            val success = pipelineInfoDao.updateAutoSummary(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                autoSummary = autoSummary,
                version = version
            )
            if (!success) {
                logger.warn(
                    "Update autoSummary for pipeline[$pipelineId] failed, " +
                        "version[$version] may not be the current version"
                )
            }
            return success
        } finally {
            lock.unlock()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAutoSummaryService::class.java)
    }
}
