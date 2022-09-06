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

import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineStatisticService @Autowired constructor(
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val dslContext: DSLContext
) {
    /**
     * 根据原子标识，获取使用该原子的pipeline个数
     */
    fun getPipelineCountByAtomCode(atomCode: String, projectCode: String?): Int {
        return pipelineModelTaskDao.getPipelineCountByAtomCode(dslContext, atomCode, projectCode)
    }

    fun batchGetPipelineCountByAtomCode(atomCodes: String, projectCode: String?): Map<String, Int> {
        val atomCodeList = atomCodes.split(",")
        val ret = mutableMapOf<String, Int>()
        // 按批次去数据库查询插件所关联的流水线数量
        ListUtils.partition(atomCodeList, 30).forEach { rids ->
            val records =
                pipelineModelTaskDao.batchGetPipelineCountByAtomCode(dslContext, rids, projectCode)
            records.map {
                ret[it.value2()] = it.value1()
            }
        }
        return ret
    }
}
