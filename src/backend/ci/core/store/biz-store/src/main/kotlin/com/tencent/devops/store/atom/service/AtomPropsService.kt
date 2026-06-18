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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.OUTPUT_DESC
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class AtomPropsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDao: AtomDao,
    private val marketAtomDao: MarketAtomDao,
    private val storeI18nMessageService: StoreI18nMessageService
) {

    fun getAtomOutput(atomCode: String): List<AtomOutput> {
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, atomCode) ?: return emptyList()
        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = atom.props,
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atom.atomCode, atom.version)
        )
        val propMap = JsonUtil.toMap(propJsonStr)
        @Suppress("UNCHECKED_CAST")
        val outputDataMap = propMap[ATOM_OUTPUT] as? Map<String, Any>
        return outputDataMap?.keys?.map { outputKey ->
            val outputDataObj = outputDataMap[outputKey]
            AtomOutput(
                name = outputKey,
                desc = if (outputDataObj is Map<*, *>) outputDataObj[OUTPUT_DESC]?.toString() else null
            )
        } ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    fun getAtomsRely(getRelyAtom: GetRelyAtom): Map<String, Map<String, Any>> {
        val atomList = marketAtomDao.getLatestAtomListByCodes(
            dslContext = dslContext,
            atomCodes = getRelyAtom.thirdPartyElementList.map { it.atomCode }
        )
        val getMap = getRelyAtom.thirdPartyElementList.map { it.atomCode to it.version }.toMap()
        val result = mutableMapOf<String, Map<String, Any>>()
        atomList.forEach lit@{
            if (it == null) return@lit
            var value = it
            val atom = getMap[it.atomCode]
            if (atom?.contains("*") == true &&
                !it.version.startsWith(atom.replace("*", ""))
            ) {
                value = atomDao.getPipelineAtom(dslContext, it.atomCode, atom) ?: return@lit
            }
            val itemMap = mutableMapOf<String, Any>()
            val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
                jsonStr = value.props,
                keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, value.atomCode, value.version)
            )
            val props: Map<String, Any> = jacksonObjectMapper().readValue(propJsonStr)
            if (null != props["input"]) {
                val input = props["input"] as? Map<String, Any>
                input?.forEach { inputIt ->
                    val paramKey = inputIt.key
                    val paramValueMap = inputIt.value as? Map<String, Any>
                    val rely = paramValueMap?.get("rely")
                    if (rely != null) {
                        itemMap[paramKey] = rely
                    }
                }
            }
            result[it.atomCode] = itemMap
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun getAtomsDefaultValue(atom: ElementThirdPartySearchParam): Map<String, Any> {
        val atomInfo = atomDao.getPipelineAtom(dslContext, atom.atomCode, atom.version) ?: return emptyMap()
        val res = mutableMapOf<String, Any>()
        val props: Map<String, Any> = jacksonObjectMapper().readValue(atomInfo.props)
        if (null != props["input"]) {
            val input = props["input"] as Map<*, *>
            input.forEach { inputIt ->
                val paramKey = inputIt.key.toString()
                val paramValueMap = inputIt.value as Map<*, *>
                if (paramValueMap["default"] != null) {
                    res[paramKey] = paramValueMap["default"]!!
                }
            }
        }
        return res
    }
}
