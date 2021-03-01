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

package com.tencent.devops.plugin.service.mooc

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.plugin.tables.TPluginMooc.T_PLUGIN_MOOC
import com.tencent.devops.plugin.dao.MoocDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MoocService @Autowired constructor(
    private val moocDao: MoocDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MoocService::class.java)
    }

    fun create(userId: String, body: Map<String, Any>): String {
        logger.info("craete mooc $userId, body: $body")
        val courseId: String = body["Class_id"]?.toString() ?: body["class_id"]?.toString() ?: ""
        return moocDao.upsert(dslContext, userId, courseId, JsonUtil.toJson(body))
    }

    fun getList(userId: String): List<Map<String, Any>> {
        val recordList = moocDao.getUserRecords(dslContext, userId)
        val result = mutableListOf<Map<String, Any>>()
        if (recordList.isNotEmpty) {
            with(T_PLUGIN_MOOC) {
                for (item in recordList) {
                    result.add(JsonUtil.toMap(item.get(PROPS)))
                }
            }
        }
        return result
    }
}
