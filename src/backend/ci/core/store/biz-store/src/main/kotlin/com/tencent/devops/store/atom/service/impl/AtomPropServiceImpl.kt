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

import com.fasterxml.jackson.core.type.TypeReference
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ThreadLocalUtil
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.AtomPropDao
import com.tencent.devops.store.atom.service.AtomPropService
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AtomPropServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomPropDao: AtomPropDao,
    private val atomDao: AtomDao,
    private val storeI18nMessageService: StoreI18nMessageService
) : AtomPropService {

    companion object {
        private const val DEFAULT_MAX_QUERY_NUM = 100
    }

    private val atomPropCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build<String, AtomProp>()

    private val atomOutputCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build<String, String>()

    @Value("\${store.maxQueryNum:100}")
    private val maxQueryNum: Int = DEFAULT_MAX_QUERY_NUM

    override fun getAtomProps(atomCodes: Set<String>): Map<String, AtomProp>? {
        var atomPropMap: MutableMap<String, AtomProp>? = null
        // 从缓存中查找插件属性信息
        var queryDbAtomCodes: MutableList<String>? = null
        val referer = BkApiUtil.getHttpServletRequest()?.getHeader(REFERER) ?: ThreadLocalUtil.get(REFERER)?.toString()
        val refererHost = referer?.let { RegexUtils.splitDomainContextPath("$referer/")?.first } ?: ""
        atomCodes.forEach { atomCode ->
            val atomProp = atomPropCache.getIfPresent("$refererHost:$atomCode")
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
                atomPropCache.put("$refererHost:$atomCode", atomProp)
            }
        }
        return atomPropMap
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAtomOutputInfos(atomInfos: Set<String>): Map<String, String>? {
        // 检查查询的梳理是否超过了系统限制
        if (atomInfos.size > maxQueryNum) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_QUERY_NUM_TOO_BIG, params = arrayOf(maxQueryNum.toString())
            )
        }
        var atomOutputInfoMap: MutableMap<String, String>? = null
        atomInfos.forEach { atomInfo ->
            // 获取请求用户的语言
            val language = I18nUtil.getRequestUserLanguage()
            val cacheKey = "$atomInfo@$language"
            // 从缓存中获取指定插件输出参数
            var outputInfo = atomOutputCache.getIfPresent(cacheKey)
            if (atomOutputInfoMap == null) {
                atomOutputInfoMap = mutableMapOf()
            }
            if (outputInfo != null) {
                atomOutputInfoMap!![atomInfo] = outputInfo
            } else {
                // 获取插件标识和版本号
                val arrays = atomInfo.split("@")
                val atomCode = arrays[0]
                val version = arrays[1]
                val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version) ?: return@forEach
                val propMap = JsonUtil.toMap(atomRecord.props)
                val outputDataMap = propMap[ATOM_OUTPUT] as? Map<String, Any>
                if (outputDataMap.isNullOrEmpty()) {
                    return@forEach
                }
                outputInfo = storeI18nMessageService.parseJsonStrI18nInfo(
                    jsonStr = JsonUtil.toJson(outputDataMap), keyPrefix = StoreUtils.getStoreFieldKeyPrefix(
                        storeType = StoreTypeEnum.ATOM, storeCode = atomCode, version = atomRecord.version
                    )
                )
                atomOutputInfoMap!![atomInfo] = outputInfo
                if (AtomStatusEnum.getProcessingStatusList().contains(atomRecord.atomStatus)) {
                    // 把状态为非流程中状态的插件版本输出信息放入缓存
                    atomOutputCache.put(cacheKey, outputInfo)
                }
            }
        }
        return atomOutputInfoMap
    }
}
