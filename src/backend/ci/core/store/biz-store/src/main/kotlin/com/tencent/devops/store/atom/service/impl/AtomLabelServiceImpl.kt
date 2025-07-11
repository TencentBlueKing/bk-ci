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

import com.tencent.devops.store.atom.dao.AtomLabelRelDao
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.atom.service.AtomLabelService
import com.tencent.devops.store.common.service.LabelService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件标签逻辑处理
 *
 * since: 2019-03-22
 */
@Service
class AtomLabelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomLabelRelDao: AtomLabelRelDao,
    private val labelService: LabelService
) : AtomLabelService {

    override fun getLabelsByAtomId(atomId: String): List<Label>? {
        return getLabelsByAtomIds(setOf(atomId))?.get(atomId)
    }

    override fun getLabelsByAtomIds(atomIds: Set<String>): Map<String, List<Label>>? {
        var atomLabelInfoMap: MutableMap<String, MutableList<Label>>? = null
        // 批量查询插件标签信息
        val atomLabelRecords = atomLabelRelDao.getLabelsByAtomIds(dslContext, atomIds)
        atomLabelRecords?.forEach { atomLabelRecord ->
            if (atomLabelInfoMap == null) {
                atomLabelInfoMap = mutableMapOf()
            }
            val atomId = atomLabelRecord[KEY_ID] as String
            if (atomLabelInfoMap!!.containsKey(atomId)) {
                val atomLabelList = atomLabelInfoMap!![atomId] ?: mutableListOf()
                labelService.addLabelToLabelList(atomLabelRecord, atomLabelList)
            } else {
                val atomLabelList = mutableListOf<Label>()
                labelService.addLabelToLabelList(atomLabelRecord, atomLabelList)
                atomLabelInfoMap!![atomId] = atomLabelList
            }
        }
        return atomLabelInfoMap
    }
}
