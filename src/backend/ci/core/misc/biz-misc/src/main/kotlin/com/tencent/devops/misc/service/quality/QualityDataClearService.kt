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

package com.tencent.devops.misc.service.quality

import com.tencent.devops.misc.dao.quality.QualityDataClearDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityDataClearService @Autowired constructor(
    private val dslContext: DSLContext,
    private val qualityDataClearDao: QualityDataClearDao
) {

    /**
     * 清除质量红线构建数据
     * @param buildId 构建ID
     *
     * 两张表均按 BUILD_ID 维度删除，互相之间无跨表一致性约束，
     * 单步失败可由下一轮 cron / 重试补救。故不再使用事务包裹，
     * 避免长事务持锁影响质量红线运行。
     */
    fun clearBuildData(buildId: String) {
        qualityDataClearDao.deleteQualityHisDetailMetadataByBuildId(dslContext, buildId)
        qualityDataClearDao.deleteQualityHisOriginMetadataByBuildId(dslContext, buildId)
    }
}
