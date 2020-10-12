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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.service.util

import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.message.MessageCode
import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder

object LocaleMessageUtils {
    private val logger = LoggerFactory.getLogger(LocaleMessageUtils::class.java)
    private val DEFAULT_MESSAGE_CODE = CommonMessageCode.SYSTEM_ERROR
    /**
     * 获取本地化消息
     * @param messageCode messageCode
     * @param params 替换描述信息占位符的参数数组
     */
    fun getLocalizedMessage(messageCode: MessageCode, params: Array<out String>?): String {
        val messageSource = SpringContextUtils.getBean(MessageSource::class.java)
        return try {
            messageSource.getMessage(messageCode.getKey(), params, getLocale())
        } catch (exception: NoSuchMessageException) {
            logger.warn("Can not find [${messageCode.getKey()}] localized message, use default message.")
            messageSource.getMessage(DEFAULT_MESSAGE_CODE.getKey(), null, getLocale())
        }
    }

    private fun getLocale(): Locale {
        return LocaleContextHolder.getLocale()
    }
}
