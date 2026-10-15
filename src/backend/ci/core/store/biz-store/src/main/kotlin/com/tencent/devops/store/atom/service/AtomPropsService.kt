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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.utils.AtomPropsCacheManager
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetAtomInputPropsRequest
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class AtomPropsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDao: AtomDao,
    private val marketAtomDao: MarketAtomDao,
    private val storeI18nMessageService: StoreI18nMessageService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomPropsService::class.java)
    }

    private data class AtomInputProps(
        // 回源是否成功，失败时 version 与 input 均不可用
        val loaded: Boolean,
        val version: String?,
        val input: Map<String, Any>
    ) {
        companion object {
            fun loadFailed() = AtomInputProps(loaded = false, version = null, input = emptyMap())
        }
    }

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

    /**
     * 批量获取插件参数定义（task.json 的 input 部分）
     *
     * 读取路径：Redis 缓存 -> 数据库（T_ATOM.PROPS），数据库回源后回填缓存。
     *
     * 通配符版本（如 1.*、*）同样读写缓存：以请求版本为 field，避免每次请求都回源数据库。
     * 回源时还会按数据库解析出的具体版本再回填一份，便于后续按具体版本命中缓存。
     * 通配版本缓存的正确性由失效机制保证：PROPS 变更时 AtomPropsCacheManager.invalidate
     * 会删除该插件整个 key，通配与具体版本一起失效。
     *
     * 回源失败的插件既不写入缓存也不出现在结果中：失败不等于「该插件没有参数」，
     * 把空结果当参数定义返回会误导调用方，写入缓存更会把错误数据保留到缓存过期。
     *
     * @return key 为「插件标识@请求版本」，value 中 key 为参数名，value 为该参数的完整定义
     * （含 default/type/label/options/rely 等），回源失败的插件不在其中
     */
    fun getAtomInputProps(getAtomInputPropsRequest: GetAtomInputPropsRequest): Map<String, Map<String, Any>> {
        val params = getAtomInputPropsRequest.thirdPartyElementList.filter {
            it.atomCode.isNotBlank() && it.version.isNotBlank()
        }
        if (params.isEmpty()) return emptyMap()

        val result = AtomPropsCacheManager.batchGetAtomInputProps(
            redisOperation = redisOperation,
            params = params
        ).toMutableMap()
        params.forEach { param ->
            val atomCode = param.atomCode
            val version = param.version
            val key = "$atomCode@$version"
            if (result.containsKey(key)) return@forEach
            val props = loadAtomInputPropsFromDb(atomCode, version)
            // 回源失败：不写入缓存，也不返回该插件的数据。
            if (!props.loaded) {
                logger.warn("load atom input props from db failed, skip it|atomCode:$atomCode|version:$version")
                return@forEach
            }
            // 按请求版本回填：通配版本也写入缓存，避免每次请求都回源数据库。
            AtomPropsCacheManager.putAtomInputProps(
                redisOperation = redisOperation,
                atomCode = atomCode,
                version = version,
                input = props.input
            )
            // 数据库解析出的具体版本与请求版本不同时（通配版本）再回填一份，便于按具体版本命中。
            props.version?.takeIf { it != version }?.let {
                AtomPropsCacheManager.putAtomInputProps(
                    redisOperation = redisOperation,
                    atomCode = atomCode,
                    version = it,
                    input = props.input
                )
            }
            result[key] = props.input
        }
        return result
    }

    /**
     * 回源数据库获取插件参数定义。
     *
     * 查无插件、PROPS 为空、PROPS 解析异常都视为回源失败，调用方不应缓存也不应使用该数据。
     *
     * @param version 请求版本，可以是具体版本也可以是通配版本
     * @return loaded 为回源是否成功；成功时 version 为数据库解析出的具体版本号，
     *         input 为该版本的 input 参数定义
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadAtomInputPropsFromDb(atomCode: String, version: String): AtomInputProps {
        val atomInfo = atomDao.getPipelineAtom(dslContext, atomCode, version)
            ?: return AtomInputProps.loadFailed()
        if (atomInfo.props.isNullOrBlank()) return AtomInputProps.loadFailed()

        // 缓存参数定义的原始内容（与 getAtomsDefaultValue 同口径，不做国际化处理），
        // 使用方按需取 default/rely 等字段
        val input = try {
            val props: Map<String, Any> = jacksonObjectMapper().readValue(atomInfo.props)
            (props[KEY_INPUT] as? Map<String, Any>)?.toMutableMap() ?: emptyMap()
        } catch (ignored: Throwable) {
            logger.warn("parse atom props error, atomCode:$atomCode|version:$version", ignored)
            return AtomInputProps.loadFailed()
        }
        return AtomInputProps(loaded = true, version = atomInfo.version, input = input)
    }
}
