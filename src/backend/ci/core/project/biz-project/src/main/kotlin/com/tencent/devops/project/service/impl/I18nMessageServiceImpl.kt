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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.model.project.tables.TI18nMessage
import com.tencent.devops.project.dao.I18nMessageDao
import com.tencent.devops.project.service.I18nMessageService
import com.tencent.devops.project.util.BkI18nMessageCacheUtil
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class I18nMessageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val i18nMessageDao: I18nMessageDao
) : I18nMessageService {

    /**
     * 批量添加国际化信息
     * @param userId 用户ID
     * @param i18nMessages 国际化信息集合
     * @return 布尔值
     */
    override fun batchAddI18nMessage(userId: String, i18nMessages: List<I18nMessage>): Boolean {
        i18nMessageDao.batchAdd(dslContext = dslContext, userId = userId, i18nMessages = i18nMessages)
        return true
    }

    /**
     * 删除用户国际化信息
     * @param userId 用户ID
     * @param moduleCode 模块标识
     * @param key 国际化变量名
     * @param language 国际化语言信息
     * @return 布尔值
     */
    override fun deleteI18nMessage(
        userId: String,
        moduleCode: String,
        key: String,
        language: String?
    ): Boolean {
        i18nMessageDao.delete(
            dslContext = dslContext,
            moduleCode = moduleCode,
            key = key,
            language = language
        )
        return true
    }

    /**
     * 查询国际化信息
     * @param moduleCode 模块标识
     * @param key 国际化变量名
     * @param language 国际化语言信息
     * @return 国际化信息
     */
    override fun getI18nMessage(
        moduleCode: String,
        key: String,
        language: String
    ): I18nMessage? {
        // 获取国际化信息缓在缓存中的key
        val i18nMessageCacheKey = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
            moduleCode = moduleCode,
            key = key,
            language = language
        )
        val value = BkI18nMessageCacheUtil.getIfPresent(i18nMessageCacheKey)
        if (value.isNullOrBlank()) {
            // 缓存中取不到则去db中查询
            val i18nMessageRecord = i18nMessageDao.get(
                dslContext = dslContext,
                moduleCode = moduleCode,
                key = key,
                language = language
            )
            return if (i18nMessageRecord != null) {
                BkI18nMessageCacheUtil.put(i18nMessageCacheKey, i18nMessageRecord.value)
                I18nMessage(moduleCode = moduleCode, language = language, key = key, value = i18nMessageRecord.value)
            } else {
                null
            }
        }
        return I18nMessage(moduleCode = moduleCode, language = language, key = key, value = value)
    }

    /**
     * 查询国际化信息集合
     * @param moduleCode 模块标识
     * @param keys 国际化变量名列表
     * @param language 国际化语言信息
     * @return 国际化信息
     */
    override fun getI18nMessages(
        moduleCode: String,
        keys: List<String>,
        language: String
    ): List<I18nMessage>? {
        var i18nMessages: MutableList<I18nMessage>? = null
        var noCacheKeys: MutableList<String>? = null
        // 1、从缓存中获取缓存的国际化信息
        keys.forEach { key ->
            var i18nMessageCacheKey = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
                moduleCode = moduleCode,
                key = key,
                language = language
            )
            var value = BkI18nMessageCacheUtil.getIfPresent(i18nMessageCacheKey)
            var keyCommonFlag = false
            val commonModuleCode = SystemModuleEnum.COMMON.name
            if (value == null && moduleCode != commonModuleCode) {
                // 如果key不存在业务模块中，则从公共模块中找
                i18nMessageCacheKey = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
                    moduleCode = commonModuleCode,
                    key = key,
                    language = language
                )
                value = BkI18nMessageCacheUtil.getIfPresent(i18nMessageCacheKey)
                keyCommonFlag = true
            }
            if (value.isNullOrBlank()) {
                if (noCacheKeys == null) {
                    noCacheKeys = mutableListOf()
                }
                // 加入未放入缓存key集合
                noCacheKeys?.add(key)
            } else {
                if (i18nMessages == null) {
                    i18nMessages = mutableListOf()
                }
                i18nMessages?.add(
                    I18nMessage(
                        moduleCode = if (keyCommonFlag) commonModuleCode else moduleCode,
                        language = language,
                        key = key,
                        value = value
                    )
                )
            }
        }
        // 2、未在缓存中的key，则批量去db中查询
        noCacheKeys?.let {
            if (i18nMessages == null) {
                i18nMessages = mutableListOf()
            }
            handleNoCacheKey(
                moduleCode = moduleCode,
                noCacheKeys = it,
                language = language,
                i18nMessages = i18nMessages!!
            )
        }
        return i18nMessages
    }

    override fun getI18nMessages(
        moduleCode: String,
        keyPrefix: String,
        language: String
    ): List<I18nMessage>? {
        val busModuleCodeName = moduleCode
        val commonModuleCodeName = SystemModuleEnum.COMMON.name
        val moduleCodes = setOf(busModuleCodeName, commonModuleCodeName)
        // 查询业务模块和公共模块key前缀为keyPrefix的记录
        val i18nMessageRecords = i18nMessageDao.list(
            dslContext = dslContext,
            moduleCodes = moduleCodes,
            keyPrefix = keyPrefix,
            language = language
        )
        var i18nMessages: MutableList<I18nMessage>? = null
        i18nMessageRecords?.forEach { i18nMessageRecord ->
            if (i18nMessages == null) {
                i18nMessages = mutableListOf()
            }
            i18nMessages!!.add(
                I18nMessage(
                    moduleCode = i18nMessageRecord.moduleCode,
                    language = language,
                    key = i18nMessageRecord.key,
                    value = i18nMessageRecord.value
                )
            )
        }
        return i18nMessages
    }

    /**
     * 处理未在缓存中的key的国际化信息
     * @param moduleCode 模块标识
     * @param noCacheKeys 未在缓存中的key集合
     * @param language 国际化语言信息
     * @param i18nMessages 国际化信息集合
     */
    private fun handleNoCacheKey(
        moduleCode: String,
        noCacheKeys: MutableList<String>,
        language: String,
        i18nMessages: MutableList<I18nMessage>
    ) {
        // 从db查找未缓存key的信息
        val busModuleCodeName = moduleCode
        val commonModuleCodeName = SystemModuleEnum.COMMON.name
        val moduleCodes = setOf(busModuleCodeName, commonModuleCodeName)
        val i18nMessageRecords = i18nMessageDao.list(
            dslContext = dslContext,
            moduleCodes = moduleCodes,
            keys = noCacheKeys,
            language = language
        )
        if (moduleCodes.size > 1) {
            // 如果要从业务模块和公共模块获取key的国际化信息，需要把业务模块的记录排在前面以便key的国际化信息优先以业务模块的为准
            val compareResult = busModuleCodeName.compareTo(commonModuleCodeName)
            val tI18nMessage = TI18nMessage.T_I18N_MESSAGE
            // 通过比较模块标识，根据比较结果排序让业务模块的记录排在前面（key同时存在业务模块和公共模块则以业务模块的记录为准）
            if (compareResult > 0) {
                i18nMessageRecords?.sortDesc(tI18nMessage.MODULE_CODE)
            } else {
                i18nMessageRecords?.sortAsc(tI18nMessage.MODULE_CODE)
            }
        }
        var handleKeys: MutableSet<String>? = null
        i18nMessageRecords?.forEach { i18nMessageRecord ->
            if (handleKeys == null) {
                handleKeys = mutableSetOf()
            }
            if (handleKeys?.contains(i18nMessageRecord.key) == true) {
                // 如处理过的key集合中包括该key，说明业务模块也有该key的记录，无需再处理公共模块的记录
                return@forEach
            }
            i18nMessages.add(
                I18nMessage(
                    moduleCode = i18nMessageRecord.moduleCode,
                    language = language,
                    key = i18nMessageRecord.key,
                    value = i18nMessageRecord.value
                )
            )
            handleKeys?.add(i18nMessageRecord.key)
            // 把db中查出来的国际化信息放入缓存
            BkI18nMessageCacheUtil.put(
                key = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
                    moduleCode = i18nMessageRecord.moduleCode,
                    key = i18nMessageRecord.key,
                    language = language
                ),
                value = i18nMessageRecord.value
            )
        }
    }
}
