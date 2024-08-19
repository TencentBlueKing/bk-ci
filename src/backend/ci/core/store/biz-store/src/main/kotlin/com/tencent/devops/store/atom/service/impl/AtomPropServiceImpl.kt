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

package com.tencent.devops.store.atom.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.store.atom.dao.AtomPropDao
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.atom.service.AtomPropService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AtomPropServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomPropDao: AtomPropDao
) : AtomPropService {

    private val atomPropCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build<String, AtomProp>()

    override fun getAtomProps(atomCodes: Set<String>): Map<String, AtomProp>? {
        var atomPropMap: MutableMap<String, AtomProp>? = null
        // 从缓存中查找插件属性信息
        var queryDbAtomCodes: MutableList<String>? = null
        atomCodes.forEach { atomCode ->
            val atomProp = atomPropCache.getIfPresent(atomCode)
            if (atomProp != null) {
                if (atomPropMap == null) {
                    atomPropMap = mutableMapOf()
                }
                atomPropMap!![atomCode] = atomProp
            } else {
                // 缓存中不存在则需要从db中查
                if (queryDbAtomCodes == null) {
                    queryDbAtomCodes = mutableListOf()
                }
                queryDbAtomCodes!!.add(atomCode)
            }
        }
        if (queryDbAtomCodes.isNullOrEmpty()) {
            // 无需从db查数据则直接返回结果数据
            return atomPropMap
        }
        ListUtils.partition(queryDbAtomCodes!!, 100).forEach { rids ->
            val atomPropRecords = atomPropDao.getAtomProps(dslContext, rids)
            if (atomPropRecords.isNullOrEmpty()) {
                return@forEach
            }
            if (atomPropMap == null) {
                atomPropMap = mutableMapOf()
            }
            val tAtom = TAtom.T_ATOM
            atomPropRecords.forEach { atomPropRecord ->
                val atomCode = atomPropRecord[tAtom.ATOM_CODE]
                var logoUrl = atomPropRecord[tAtom.LOGO_URL]
                logoUrl = logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
                }
                logoUrl = RegexUtils.trimProtocol(logoUrl)
                val atomProp = AtomProp(
                    atomCode = atomCode,
                    os = JsonUtil.to(atomPropRecord[tAtom.OS], object : TypeReference<List<String>>() {}),
                    logoUrl = logoUrl,
                    buildLessRunFlag = atomPropRecord[tAtom.BUILD_LESS_RUN_FLAG]
                )
                atomPropMap!![atomCode] = atomProp
                // 把数据放入缓存
                atomPropCache.put(atomCode, atomProp)
            }
        }
        return atomPropMap
    }
}
