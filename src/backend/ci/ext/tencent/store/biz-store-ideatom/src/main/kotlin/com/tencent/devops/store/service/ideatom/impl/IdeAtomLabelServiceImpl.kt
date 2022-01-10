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

package com.tencent.devops.store.service.ideatom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.ideatom.IdeAtomLabelRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.service.common.LabelService
import com.tencent.devops.store.service.ideatom.IdeAtomLabelService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IdeAtomLabelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val ideAtomLabelRelDao: IdeAtomLabelRelDao,
    private val labelService: LabelService
) : IdeAtomLabelService {

    private val logger = LoggerFactory.getLogger(IdeAtomLabelServiceImpl::class.java)

    /**
     * 查找IDE插件标签
     */
    override fun getLabelsByAtomId(atomId: String): Result<List<Label>?> {
        logger.info("getLabelsByAtomId atomId is :$atomId")
        val ideAtomLabelList = mutableListOf<Label>()
        val ideAtomLabelRecords = ideAtomLabelRelDao.getLabelsByAtomId(dslContext, atomId) // 查询IDE插件标签信息
        ideAtomLabelRecords?.forEach {
            labelService.addLabelToLabelList(it, ideAtomLabelList)
        }
        return Result(ideAtomLabelList)
    }
}
