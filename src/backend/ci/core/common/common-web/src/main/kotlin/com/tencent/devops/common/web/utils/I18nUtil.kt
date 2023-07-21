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

package com.tencent.devops.common.web.utils

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SERVICE_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.REQUEST_CHANNEL
import com.tencent.devops.common.api.enums.RequestChannelTypeEnum
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.LocaleUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.CookieUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.ServiceLocaleResource
import java.net.URLDecoder
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object I18nUtil {

    private val logger = LoggerFactory.getLogger(I18nUtil::class.java)

    private val syncObj = Object()

    /**
     * 从redis缓存获取用户的国际化语言信息
     * @param userId 用户ID
     * @return 语言信息
     */
    private fun getUserLocaleLanguageFromCache(userId: String): String? {
        // 先从Cookie获取,没有再从本地缓存中获取用户设置的语言信息
        var language = getCookieLocale() ?: BkI18nLanguageCacheUtil.getIfPresent(userId)
        if (language.isNullOrBlank()) {
            // 本地缓存中获取不到语言信息再从redis中获取
            val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
            language = redisOperation.get(LocaleUtil.getUserLocaleLanguageKey(userId))
            if (!language.isNullOrBlank()) {
                // 如果redis中用户语言不为空，则把用户的语言缓存到本地
                BkI18nLanguageCacheUtil.put(userId, language)
            }
        }
        return language
    }

    private var devopsDefaultLocaleLanguage: String? = null // 部署配置不会动态变化, 运行时可一次解析固化.

    /**
     * 获取蓝盾默认支持的语言
     * @return 系统默认语言
     */
    fun getDefaultLocaleLanguage(): String {
        return devopsDefaultLocaleLanguage ?: run {
            synchronized(syncObj) {
                if (devopsDefaultLocaleLanguage.isNullOrBlank()) {
                    val commonConfig: CommonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
                    devopsDefaultLocaleLanguage = commonConfig.devopsDefaultLocaleLanguage
                }
            }
            devopsDefaultLocaleLanguage ?: DEFAULT_LOCALE_LANGUAGE
        }
    }

    /**
     * 获取接口请求渠道信息
     * @return 渠道信息
     */
    private fun getRequestChannel(): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return if (null != attributes) {
            val request = attributes.request
            (request.getAttribute(REQUEST_CHANNEL) ?: request.getHeader(REQUEST_CHANNEL))?.toString()
        } else {
            null // 不是接口请求来源则返回null
        }
    }

    /**
     * 获取Http Cookie中用户携带的语言
     * @return 语言,如果没有,则为空
     */
    private fun getCookieLocale(): String? {
        // 从request请求中获取本地语言信息
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes?.let { bkLanguageTransMap[CookieUtil.getCookieValue(attributes.request, BK_LANGUAGE)] }
    }

    // 蓝鲸专定义的语言头, 有差异,要定制转换
    private const val BK_LANGUAGE = "blueking_language"
    private val bkLanguageTransMap = mapOf(
        "zh-cn" to "zh_CN",
        "zh-CN" to "zh_CN",
        "en" to "en_US",
        "en-US" to "en_US",
        "zh-HK" to "zh_HK",
        "zh-hk" to "zh_HK",
        "zh-tw" to "zh_TW",
        "zh-TW" to "zh_TW",
        "zh-mo" to "zh_MO",
        "zh-MO" to "zh_MO",
        "zh-sg" to "zh_SG",
        "zh-SG" to "zh_SG"
    )

    /**
     * 获取接口请求用户信息
     * @return 用户ID
     */
    fun getRequestUserId(): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return if (null != attributes) {
            val request = attributes.request
            request.getHeader(AUTH_HEADER_USER_ID)?.toString()
        } else {
            null
        }
    }

    /**
     * 获取语言信息
     * @param userId 用户ID
     * @return 语言信息
     */
    fun getLanguage(userId: String? = null): String {
        val defaultLanguage = getDefaultLocaleLanguage()
        userId ?: return defaultLanguage
        val requestChannel = getRequestChannel()
        return if (requestChannel != RequestChannelTypeEnum.BUILD.name) {
            // 如果请求来源是build接口，先从缓存中获取用户获取的语言信息
            try {
                var language = getUserLocaleLanguageFromCache(userId)
                if (language.isNullOrBlank()) {
                    // 缓存中未取到语言则通过接口从db中获取用户设置的语言信息（db中也没有语言信息则给该用户的语言设置为默认语言）
                    val client = SpringContextUtil.getBean(Client::class.java)
                    language =
                        client.get(ServiceLocaleResource::class).getUserLocale(userId).data?.language ?: defaultLanguage
                    val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
                    // 把查出来的用户语言放入缓存
                    BkI18nLanguageCacheUtil.put(userId, language)
                    redisOperation.set(LocaleUtil.getUserLocaleLanguageKey(userId), language)
                }
                language
            } catch (ignored: Throwable) {
                logger.warn("Fail to get language of userId[$userId]", ignored)
                defaultLanguage
            }
        } else {
            defaultLanguage
        }
    }

    /**
     * 获取用户语言信息
     * @return 用户语言信息
     */
    fun getRequestUserLanguage() = getLanguage(getRequestUserId())

    /**
     * 根据语言环境获取对应的描述信息
     * @param messageCode 消息标识
     * @param language 语言信息
     * @param params 替换描述信息占位符的参数数组
     * @param defaultMessage 默认信息
     * @param checkUrlDecoder 考虑利用URL编码以支持多行信息，以及带特殊字符的信息
     * @return 描述信息
     */
    fun getCodeLanMessage(
        messageCode: String,
        language: String? = null,
        params: Array<String>? = null,
        defaultMessage: String? = null,
        checkUrlDecoder: Boolean = false
    ): String {
        // 获取国际化语言信息
        val i18nLanguage = if (language.isNullOrBlank()) {
            // 方法没传语言信息，则通过获取请求头中用户信息对应的语言信息
            getLanguage(getRequestUserId())
        } else {
            language
        }
        val i18nMessage = MessageUtil.getMessageByLocale(
            messageCode = messageCode,
            language = i18nLanguage,
            params = params,
            defaultMessage = defaultMessage
        )
        return if (i18nMessage.isNotBlank() && checkUrlDecoder) {
            URLDecoder.decode(i18nMessage, "UTF-8")
        } else {
            i18nMessage
        }
    }

    /**
     * 生成请求响应对象
     * @param messageCode 状态码
     * @param params 替换状态码描述信息占位符的参数数组
     * @param data 数据对象
     * @return Result响应结果对象
     */
    fun <T> generateResponseDataObject(
        messageCode: String,
        params: Array<String>? = null,
        data: T? = null,
        language: String? = null,
        defaultMessage: String? = null
    ): Result<T> {
        val message = getCodeLanMessage(
            messageCode = messageCode,
            language = language,
            params = params,
            defaultMessage = defaultMessage
        )
        // 生成Result对象
        return Result(messageCode.toInt(), message, data)
    }

    /**
     * 获取模块标识
     * @param attributes 属性列表
     * @return 模块标识
     */
    fun getModuleCode(attributes: ServletRequestAttributes?): String {
        val moduleCode = if (null != attributes) {
            val request = attributes.request
            // 从请求头中获取服务名称
            val serviceName = request.getHeader(AUTH_HEADER_DEVOPS_SERVICE_NAME) ?: SystemModuleEnum.COMMON.name
            try {
                serviceName.uppercase()
            } catch (ignored: Throwable) {
                logger.warn("serviceName[${serviceName.uppercase()}] is invalid", ignored)
                SystemModuleEnum.COMMON.name
            }
        } else {
            // 默认从公共模块获取国际化信息
            SystemModuleEnum.COMMON.name
        }
        return moduleCode
    }
}
