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
    override fun batchAddI18nMessage(userId: String, i18nMessages: List<I18nMessage>): Boolean {
        i18nMessageDao.batchAdd(dslContext = dslContext, userId = userId, i18nMessages = i18nMessages)
        return true
    }

    override fun deleteI18nMessage(
        userId: String,
        moduleCode: SystemModuleEnum,
        key: String,
        locale: String?
    ): Boolean {
        i18nMessageDao.delete(
            dslContext = dslContext,
            moduleCode = moduleCode,
            key = key,
            locale = locale
        )
        return true
    }

    override fun getI18nMessage(
        userId: String,
        moduleCode: SystemModuleEnum,
        key: String,
        locale: String
    ): I18nMessage? {
        // 获取国际化信息缓在缓存中的key
        val i18nMessageCacheKey = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
            moduleCode = moduleCode.name,
            key = key,
            locale = locale
        )
        val value = BkI18nMessageCacheUtil.getIfPresent(i18nMessageCacheKey)
        if (value.isNullOrBlank()) {
            // 缓存中取不到则去db中查询
            val i18nMessageRecord = i18nMessageDao.get(
                dslContext = dslContext,
                moduleCode = moduleCode,
                key = key,
                locale = locale
            )
            return if (i18nMessageRecord != null) {
                BkI18nMessageCacheUtil.put(i18nMessageCacheKey, i18nMessageRecord.value)
                I18nMessage(moduleCode = moduleCode, locale = locale, key = key, value = i18nMessageRecord.value)
            } else {
                null
            }
        }
        return I18nMessage(moduleCode = moduleCode, locale = locale, key = key, value = value)
    }

    override fun getI18nMessages(
        userId: String,
        moduleCode: SystemModuleEnum,
        keys: List<String>,
        locale: String
    ): List<I18nMessage>? {
        var i18nMessages: MutableList<I18nMessage>? = null
        var noCacheKeys: MutableList<String>? = null
        // 1、从缓存中获取缓存的国际化信息
        keys.forEach { key ->
            val i18nMessageCacheKey = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
                moduleCode = moduleCode.name,
                key = key,
                locale = locale
            )
            val value = BkI18nMessageCacheUtil.getIfPresent(i18nMessageCacheKey)
            if (value.isNullOrBlank()) {
                if (noCacheKeys == null) {
                    noCacheKeys = mutableListOf()
                }
                // 加入未放入缓存key集合
                noCacheKeys!!.add(key)
            } else {
                if (i18nMessages == null) {
                    i18nMessages = mutableListOf()
                }
                i18nMessages!!.add(I18nMessage(moduleCode = moduleCode, locale = locale, key = key, value = value))
            }
        }
        // 2、未在缓存中的key，则批量去db中查询
        noCacheKeys?.let {
            val i18nMessageRecords = i18nMessageDao.list(
                dslContext = dslContext,
                moduleCode = moduleCode,
                keys = it,
                locale = locale
            )
            i18nMessageRecords?.forEach { i18nMessageRecord ->
                if (i18nMessages == null) {
                    i18nMessages = mutableListOf()
                }
                i18nMessages!!.add(
                    I18nMessage(
                        moduleCode = moduleCode,
                        locale = locale,
                        key = i18nMessageRecord.key,
                        value = i18nMessageRecord.value
                    )
                )
                // 把db中查出来的国际化信息放入缓存
                BkI18nMessageCacheUtil.put(
                    key = BkI18nMessageCacheUtil.getI18nMessageCacheKey(
                        moduleCode = moduleCode.name,
                        key = i18nMessageRecord.key,
                        locale = locale
                    ),
                    value = i18nMessageRecord.value
                )
            }
        }
        return i18nMessages
    }
}
