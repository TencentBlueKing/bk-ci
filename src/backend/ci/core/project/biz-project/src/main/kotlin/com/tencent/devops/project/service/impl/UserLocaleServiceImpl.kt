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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.LocaleUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.project.dao.UserLocaleDao
import com.tencent.devops.common.api.pojo.LocaleInfo
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.pojo.LanguageInfo
import com.tencent.devops.project.service.UserLocaleService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserLocaleServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val userLocaleDao: UserLocaleDao,
    private val redisOperation: RedisOperation,
    private val commonConfig: CommonConfig
) : UserLocaleService {

    override fun addUserLocale(userId: String, language: String): Boolean {
        val key = LocaleUtil.getUserLocaleLanguageKey(userId)
        val lock = RedisLock(redisOperation, "$key:add", 10)
        try {
            lock.lock()
            val localeCount = userLocaleDao.countLocaleByUserId(
                dslContext = dslContext,
                userId = userId
            )
            if (localeCount > 0) {
                // 已添加则无需重复添加
                return true
            }
            // locale语言信息入库
            userLocaleDao.add(dslContext, userId, language)
            // locale语言信息写入redis缓存
            redisOperation.set(
                key = key,
                value = language
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun deleteUserLocale(userId: String): Boolean {
        userLocaleDao.delete(dslContext, userId)
        redisOperation.delete(LocaleUtil.getUserLocaleLanguageKey(userId))
        return true
    }

    override fun updateUserLocale(userId: String, language: String): Boolean {
        // 获取蓝盾支持的语言列表
        val supportLanguages = commonConfig.devopsSupportLanguages.split(",")
        if (!supportLanguages.contains(language)) {
            // 如果更新的语言不在支持的语言列表中则给出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_LANGUAGE_IS_NOT_SUPPORT,
                params = supportLanguages.toTypedArray()
            )
        }
        val key = LocaleUtil.getUserLocaleLanguageKey(userId)
        val lock = RedisLock(redisOperation, "$key:update", 10)
        try {
            lock.lock()
            // 更新db中locale语言信息
            userLocaleDao.update(dslContext, userId, language)
            // 更新redis缓存locale语言信息
            redisOperation.set(
                key = key,
                value = language
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun getUserLocale(userId: String): LocaleInfo {
        val key = LocaleUtil.getUserLocaleLanguageKey(userId)
        // 从缓存中获取locale语言信息
        var language = redisOperation.get(key)
        if (language.isNullOrBlank()) {
            // 缓存中未取到则直接从db查
            language = userLocaleDao.getLocaleByUserId(dslContext, userId)
            if (language.isNullOrBlank()) {
                // 用户没有设置国际化语言信息，则给该用户分配系统默认的语言信息
                language = commonConfig.devopsDefaultLocaleLanguage
                addUserLocale(userId, language)
            }
        }
        // 用户未配置locale语言信息则默认返回系统默认配置
        return LocaleInfo(language)
    }

    override fun listSupportLanguages(userId: String): List<LanguageInfo> {
        // 获取蓝盾支持的语言列表
        val supportLanguages = commonConfig.devopsSupportLanguages.split(",")
        val languageInfos = mutableListOf<LanguageInfo>()
        supportLanguages.forEach { language ->
            languageInfos.add(
                LanguageInfo(
                    language = language,
                    name = I18nUtil.getCodeLanMessage(messageCode = "language.$language", defaultMessage = language)
                )
            )
        }
        return languageInfos
    }
}
