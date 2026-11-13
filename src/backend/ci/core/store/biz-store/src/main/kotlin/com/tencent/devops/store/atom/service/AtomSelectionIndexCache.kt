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

package com.tencent.devops.store.atom.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.store.atom.util.AtomSelectScorer
import com.tencent.devops.store.common.dao.StoreComponentContractDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 插件契约选型字段的本地缓存。多实例部署时其他实例最多滞后一个过期周期
 */
@Component
class AtomSelectionIndexCache @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeComponentContractDao: StoreComponentContractDao
) {

    private val cache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String, List<AtomSelectScorer.Entry>>()

    fun entries(): List<AtomSelectScorer.Entry> = cache.get(KEY) { load() }

    fun invalidate() = cache.invalidate(KEY)

    private fun load(): List<AtomSelectScorer.Entry> {
        val records = storeComponentContractDao.listSelectionFields(dslContext, StoreTypeEnum.ATOM)
        val entries = records.map {
            AtomSelectScorer.Entry(
                atomCode = it.value1(),
                summary = it.value2(),
                keywords = AtomSelectScorer.parseKeywords(it.value3()),
                coverage = it.value4(),
                stale = it.value5() == true
            )
        }
        logger.info("atom selection index loaded|${entries.size}")
        return entries
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomSelectionIndexCache::class.java)
        private const val KEY = "ATOM"
        private const val EXPIRE_MINUTES = 5L
    }
}
