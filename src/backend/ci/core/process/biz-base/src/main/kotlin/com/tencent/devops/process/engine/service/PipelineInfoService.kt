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

import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.store.pojo.common.ATOM_SENSITIVE_PARAM_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_NORMAL_PROJECT_RUN_INFO_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation
) {

    fun getPipelineName(projectId: String, pipelineId: String): String? {
        return pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)?.pipelineName
    }

    // 敏感入参解析
    fun transferSensitiveParam(projectTestAtomCodes: List<String>, element: Element) {
        if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
            val atomCode = element.getAtomCode()
            val version = element.version
            val hashKey = if (version.contains(".*")) {
                var latestVersion: String? = null
                if (projectTestAtomCodes.contains(atomCode)) {
                    latestVersion = version
                }
                if (latestVersion.isNullOrBlank()) {
                    val atomRunInfoStr = redisOperation.hget(
                        key = "$STORE_NORMAL_PROJECT_RUN_INFO_KEY_PREFIX:${StoreTypeEnum.ATOM.name}:$atomCode",
                        hashKey = version
                    )
                    val atomRunInfo = atomRunInfoStr?.let { JsonUtil.toMap(it) }
                    latestVersion = atomRunInfo?.get(KEY_VERSION).toString()
                }
                latestVersion
            } else {
                version
            }
            val param = redisOperation.hget(
                key = "$ATOM_SENSITIVE_PARAM_KEY_PREFIX:$atomCode",
                hashKey = hashKey
            )
            if (!param.isNullOrBlank()) {
                element.transferSensitiveParam(param.split(","))
            }
        }
    }
}
