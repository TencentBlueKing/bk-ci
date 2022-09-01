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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PipelineAsCodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAsCodeService::class.java)

        private const val ENABLE_PIPELINE_AS_CODE_CACHE_MAX_SIZE = 100000L
        private const val ENABLE_PIPELINE_AS_CODE_EXPIRE_MINUTES = 15L
        private const val CACHE_KEY_DELIMITER = ":as:code:pipeline:"
        private fun getPipelineCacheKey(projectId: String, buildId: String) = "$projectId$CACHE_KEY_DELIMITER$buildId"
        private fun getPipelineIdByCacheKey(cacheKey: String): Pair<String, String> {
            val split = cacheKey.split(CACHE_KEY_DELIMITER)
            return Pair(split.first(), split.last())
        }
    }

    private val asCodeEnabledCache = Caffeine.newBuilder()
        .maximumSize(ENABLE_PIPELINE_AS_CODE_CACHE_MAX_SIZE)
        .expireAfterAccess(ENABLE_PIPELINE_AS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String/*projectAndPipelineId*/, Boolean/*enabled*/> { pipelineKey ->
            val (projectId, pipelineId) = getPipelineIdByCacheKey(pipelineKey)
            val result = getPipelineAsCodeSettings(projectId, pipelineId)?.enable == true
            logger.info("[$projectId][$pipelineId]|setEnabledCache|$pipelineKey=$result")
            result
        }

    fun asCodeEnabled(
        projectId: String,
        pipelineId: String
    ) = asCodeEnabledCache.get(getPipelineCacheKey(projectId, pipelineId))

    fun getPipelineAsCodeSettings(projectId: String, pipelineId: String): PipelineAsCodeSettings? {
        return pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
            ?.pipelineAsCodeSettings?.let { self ->
                JsonUtil.to(self, PipelineAsCodeSettings::class.java)
            }
    }
}
