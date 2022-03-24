/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.pojo.GlobalMessage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.util.JsonUtil
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.text.MessageFormat

/**
 * 返回状态码工具类
 * @since: 2018-11-10
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
@Component
class MessageCodeUtil @Autowired constructor() {
    companion object {
        private val logger = LoggerFactory.getLogger(MessageCodeUtil::class.java)

        /**
         * 生成请求响应对象
         * @param messageCode 状态码
         */
        fun <T> generateResponseDataObject(
            messageCode: String
        ): Result<T> {
            return generateResponseDataObject(messageCode, null, null)
        }

        /**
         * 生成请求响应对象
         * @param messageCode 状态码
         * @param data 数据对象
         */
        fun <T> generateResponseDataObject(
            messageCode: String,
            data: T?
        ): Result<T> {
            return generateResponseDataObject(messageCode, null, data)
        }

        /**
         * 生成请求响应对象
         * @param messageCode 状态码
         * @param params 替换状态码描述信息占位符的参数数组
         */
        fun <T> generateResponseDataObject(
            messageCode: String,
            params: Array<String>
        ): Result<T> {
            return generateResponseDataObject(messageCode, params, null)
        }

        /**
         * 生成请求响应对象
         * @param messageCode 状态码
         * @param params 替换状态码描述信息占位符的参数数组
         * @param data 数据对象
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> generateResponseDataObject(
            messageCode: String,
            params: Array<String>?,
            data: T?
        ): Result<T> {
            var message: String? = null
            try {
                val redisTemplate: RedisTemplate<String, String> = SpringContextUtil.getBean(
                    RedisTemplate::class.java,
                    "stringRedisTemplate"
                ) as RedisTemplate<String, String>
                // 根据状态码从redis中获取该状态码对应的信息
                val messageCodeDetailStr = redisTemplate.opsForValue().get(messageCode)
                if (StringUtils.isNotEmpty(messageCodeDetailStr)) {
                    // 获取字符集（与http请求头中的Accept-Language有关）
                    val locale = LocaleContextHolder.getLocale().toString()
                    val messageCodeDetail =
                        JsonUtil.getObjectMapper().readValue(messageCodeDetailStr, GlobalMessage::class.java)
                    // 根据字符集取出对应的状态码描述信息
                    message = getMessageByLocale(messageCodeDetail, locale)
                    val arguments = params ?: arrayOf("")
                    // 根据参数动态替换状态码描述里的占位符
                    message = MessageFormat.format(message, *arguments)
                }
            } catch (e: Exception) {
                logger.error("get message error is :$e")
                message = "System service busy, please try again later. {0}"
            }
            return Result(
                status = messageCode.toInt(),
                message = message,
                data = data
            ) // 生成Result对象
        }

        private fun getMessageByLocale(globalMessage: GlobalMessage, locale: String): String {
            return if ("ZH_CN".equals(locale, true)) {
                globalMessage.messageZhCn // 简体中文描述
            } else if ("ZH_TW".equals(locale, true) || "ZH_HK".equals(locale, true)) {
                globalMessage.messageZhTw ?: "" // 繁体中文描述
            } else {
                globalMessage.messageEn ?: "" // 英文描述
            }
        }
    }
}
