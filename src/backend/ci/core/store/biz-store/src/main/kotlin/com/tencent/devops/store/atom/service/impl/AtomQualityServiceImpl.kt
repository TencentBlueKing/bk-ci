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

package com.tencent.devops.store.atom.service.impl

import com.tencent.devops.common.api.constant.IN_READY_TEST
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.quality.api.v2.ServiceQualityControlPointMarketResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorMarketResource
import com.tencent.devops.quality.api.v2.ServiceQualityMetadataMarketResource
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.atom.service.AtomQualityService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件质量红线逻辑处理
 *
 * since: 2019-03-22
 */
@Service
class AtomQualityServiceImpl @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation
) : AtomQualityService {

    private val logger = LoggerFactory.getLogger(AtomQualityServiceImpl::class.java)

    override fun updateQualityInApprove(atomCode: String, atomStatus: Byte) {
        logger.info("update quality params: [$atomCode|$atomStatus]")

        val key = this::class.java.name + "#" + Thread.currentThread().stackTrace[1].methodName + "#" + atomCode
        val lock = RedisLock(redisOperation, key, 3600L)

        try {
            if (!lock.tryLock()) {
                throw IllegalArgumentException("get lock failed and do not do update quality in approve")
            }

            if (atomStatus == AtomStatusEnum.RELEASED.status.toByte()) {
                // 审核通过就刷新基础数据和指标
                val metadataMap =
                    client.get(ServiceQualityMetadataMarketResource::class).refreshMetadata(atomCode).data ?: mapOf()
                client.get(ServiceQualityIndicatorMarketResource::class).refreshIndicator(atomCode, metadataMap)
                client.get(ServiceQualityControlPointMarketResource::class).refreshControlPoint(atomCode)
            }
            // 删除测试数据
            client.get(ServiceQualityMetadataMarketResource::class).deleteTestMetadata(atomCode, IN_READY_TEST)
            client.get(ServiceQualityIndicatorMarketResource::class).deleteTestIndicator(atomCode, IN_READY_TEST)
            client.get(ServiceQualityControlPointMarketResource::class).deleteTestControlPoint(atomCode, IN_READY_TEST)
        } finally {
            lock.unlock()
        }
    }
}
