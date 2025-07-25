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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.store.api.common.ServiceStoreStatisticResource
import com.tencent.devops.store.pojo.common.statistic.StoreStatisticPipelineNumUpdate
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线插件统计相关的服务
 * @version 1.0
 */
@Service
class PipelineAtomStatisticsService @Autowired constructor(
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomStatisticsService::class.java)
    }

    /**
     * 更新插件对应的流水线数量
     */
    fun updateAtomPipelineNum(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        deleteFlag: Boolean = false,
        restoreFlag: Boolean = false
    ) {
        val pipelineNumUpdateList = mutableListOf<StoreStatisticPipelineNumUpdate>()
        val currentVersionModelStr = getVersionModelString(projectId, pipelineId, version) ?: return
        val currentVersionModel = JsonUtil.to(currentVersionModelStr, Model::class.java)
        // 获取当前流水线版本模型中插件的集合（去掉重复插件）
        val currentVersionAtomSet = ModelUtils.getModelAtoms(currentVersionModel)
        val lock = RedisLock(redisOperation, "$pipelineId:updateAtomPipelineNum", 60)
        try {
            lock.lock()
            when {
                deleteFlag -> {
                    addPipelineNumUpdate(currentVersionAtomSet, pipelineNumUpdateList, false)
                }
                else -> {
                    if (version == null) {
                        return
                    }
                    if (version > 1 && !restoreFlag) {
                        val lastVersionModelStr = getVersionModelString(projectId, pipelineId, version - 1) ?: return
                        val lastVersionModel = JsonUtil.to(lastVersionModelStr, Model::class.java)
                        // 获取上一个流水线版本模型中插件的集合（去掉重复插件）
                        val lastVersionAtomSet = ModelUtils.getModelAtoms(lastVersionModel)
                        val dataList = mutableSetOf<String>()
                        dataList.addAll(currentVersionAtomSet)
                        // 获取当前版本新增插件集合
                        currentVersionAtomSet.removeAll(lastVersionAtomSet)
                        addPipelineNumUpdate(currentVersionAtomSet, pipelineNumUpdateList, true)
                        // 获取当前版本删除插件集合
                        lastVersionAtomSet.removeAll(dataList)
                        addPipelineNumUpdate(lastVersionAtomSet, pipelineNumUpdateList, false)
                    } else {
                        addPipelineNumUpdate(currentVersionAtomSet, pipelineNumUpdateList, true)
                    }
                }
            }
            // 更新store统计表插件对应的流水线的数量
            val pipelineNumUpdateResult =
                client.get(ServiceStoreStatisticResource::class)
                    .updatePipelineNum(StoreTypeEnum.ATOM, pipelineNumUpdateList)
            if (pipelineNumUpdateResult.isNotOk()) {
                logger.warn(
                    "updateAtomPipelineNum pipelineId:$pipelineId,version:$version," +
                            "pipelineNumUpdateResult:$pipelineNumUpdateResult"
                )
                throw ErrorCodeException(
                    errorCode = pipelineNumUpdateResult.status.toString(),
                    defaultMessage = pipelineNumUpdateResult.message
                )
            }
        } finally {
            lock.unlock()
        }
    }

    private fun addPipelineNumUpdate(
        modelVersionAtomSet: MutableSet<String>,
        pipelineNumUpdateList: MutableList<StoreStatisticPipelineNumUpdate>,
        incrementFlag: Boolean
    ) {
        modelVersionAtomSet.forEach { atomCode ->
            pipelineNumUpdateList.add(
                StoreStatisticPipelineNumUpdate(atomCode, incrementFlag)
            )
        }
    }

    private fun getVersionModelString(projectId: String, pipelineId: String, version: Int?): String? {
        return pipelineResourceVersionDao.getVersionModelString(dslContext, projectId, pipelineId, version)
    }
}
