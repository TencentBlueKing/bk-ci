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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.dao.StoreLabelRelDao
import com.tencent.devops.store.common.service.LabelService
import com.tencent.devops.store.common.service.StoreLabelService
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.label.Label
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件标签逻辑处理
 *
 * since: 2019-03-22
 */
@Service
class StoreLabelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeLabelRelDao: StoreLabelRelDao,
    private val labelService: LabelService
) : StoreLabelService {

    override fun getLabelsByStoreId(storeId: String): List<Label>? {
        return getLabelsByStoreIds(setOf(storeId))?.get(storeId)
    }

    override fun getLabelsByStoreIds(storeIds: Set<String>): Map<String, List<Label>>? {
        var storeLabelInfoMap: MutableMap<String, MutableList<Label>>? = null
        // 批量查询插件标签信息
        val storeLabelRecords = storeLabelRelDao.getLabelsByStoreIds(dslContext, storeIds)
        storeLabelRecords?.forEach { storeLabelRecord ->
            if (storeLabelInfoMap == null) {
                storeLabelInfoMap = mutableMapOf()
            }
            val storeId = storeLabelRecord[KEY_ID] as String
            if (storeLabelInfoMap!!.containsKey(storeId)) {
                val storeLabelList = storeLabelInfoMap!![storeId] ?: mutableListOf()
                labelService.addLabelToLabelList(storeLabelRecord, storeLabelList)
            } else {
                val storeLabelList = mutableListOf<Label>()
                labelService.addLabelToLabelList(storeLabelRecord, storeLabelList)
                storeLabelInfoMap!![storeId] = storeLabelList
            }
        }
        return storeLabelInfoMap
    }
}
