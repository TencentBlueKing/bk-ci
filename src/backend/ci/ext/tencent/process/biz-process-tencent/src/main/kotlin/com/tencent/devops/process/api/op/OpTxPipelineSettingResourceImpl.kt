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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTxPipelineSettingResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao
) : OpTxPipelineSettingResource {

    override fun updatePipelineSettingContent(userId: String): Result<Boolean> {
        val startTime = LocalDateTime.of(2023, 5, 18, 15, 0)
        val endTime = LocalDateTime.now()
        var page = PageUtil.DEFAULT_PAGE
        val pageSize = PageUtil.MAX_PAGE_SIZE
        logger.info("start update pipeline setting content")
        do {
            val pipelineIds = pipelineInfoDao.getIdByCreateTimePeriod(
                dslContext = dslContext,
                startTime = startTime,
                endTime = endTime,
                page = page,
                pageSize = pageSize
            )
            val tPipelineSettingVersions = mutableListOf<TPipelineSettingVersionRecord>()
            pipelineSettingVersionDao.getSettingByPipelineIds(
                dslContext,
                pipelineIds
            ).forEach {
                val newSuccessContent = replaceContent(it.successContent)
                val newFailContent = replaceContent(it.failContent)
                if (newSuccessContent != it.successContent || newFailContent != it.failContent) {
                    it.successContent = newSuccessContent
                    it.failContent = newFailContent
                    tPipelineSettingVersions.add(it)
                }
            }
            if (tPipelineSettingVersions.isNotEmpty()) {
                pipelineSettingVersionDao.batchUpdate(dslContext, tPipelineSettingVersions)
            }
            page ++
        } while (pipelineIds.size == pageSize)
        logger.info("update pipeline setting content end")
        return Result(true)
    }

    private fun replaceContent(content: String): String {
        var newContent = content
        if (newContent.contains(projectNameChinese)) {
                newContent = newContent.replace(projectNameChinese, "\${$PROJECT_NAME_CHINESE}")
        }
        if (newContent.contains(pipelineName)) {
            newContent = newContent.replace(pipelineName, "\${$PIPELINE_NAME}")
        }
        if (newContent.contains(pipelineBuildNum)) {
            newContent = newContent.replace(pipelineBuildNum, "\${$PIPELINE_BUILD_NUM}")
        }
        if (newContent.contains(pipelineTimeDuration)) {
            newContent = newContent.replace(pipelineTimeDuration, "\${$PIPELINE_TIME_DURATION}")
        }
        if (newContent.contains(pipelineStartUserName)) {
            newContent = newContent.replace(pipelineStartUserName, "\${$PIPELINE_START_USER_NAME}")
        }
        return newContent
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpTxPipelineSettingResourceImpl::class.java)
        private const val projectNameChinese = "\$$PROJECT_NAME_CHINESE"
        private const val pipelineName = "\$$PIPELINE_NAME"
        private const val pipelineBuildNum = "\$$PIPELINE_BUILD_NUM"
        private const val pipelineTimeDuration = "\$$PIPELINE_TIME_DURATION"
        private const val pipelineStartUserName = "\$$PIPELINE_START_USER_NAME"
    }
}
