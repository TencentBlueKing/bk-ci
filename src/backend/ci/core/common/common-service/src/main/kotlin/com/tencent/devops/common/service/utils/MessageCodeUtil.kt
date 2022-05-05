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

package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.constant.BCI_CODE_PREFIX
import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.text.MessageFormat

object MessageCodeUtil {
    private val logger = LoggerFactory.getLogger(MessageCodeUtil::class.java)

    /**
     * 生成请求响应对象
     * @param messageCode 状态码
     * @return Result响应结果对象
     */
    fun <T> generateResponseDataObject(
        messageCode: String
    ): Result<T> = generateResponseDataObject(messageCode = messageCode, params = null, data = null)

    /**
     * 生成请求响应对象
     * @param messageCode 状态码
     * @param data 数据对象
     * @return Result响应结果对象
     */
    fun <T> generateResponseDataObject(
        messageCode: String,
        data: T?
    ): Result<T> = generateResponseDataObject(messageCode = messageCode, params = null, data = data)

    /**
     * 生成请求响应对象
     * @param messageCode 状态码
     * @param params 替换状态码描述信息占位符的参数数组
     * @return Result响应结果对象
     */
    fun <T> generateResponseDataObject(
        messageCode: String,
        params: Array<String>?
    ): Result<T> = generateResponseDataObject(messageCode, params, data = null)

    /**
     * 生成请求响应对象
     * @param messageCode 状态码
     * @param params 替换状态码描述信息占位符的参数数组
     * @param data 数据对象
     * @return Result响应结果对象
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> generateResponseDataObject(
        messageCode: String,
        params: Array<String>?,
        data: T?,
        defaultMessage: String? = null
    ): Result<T> {
        val message = getCodeMessage(messageCode, params) ?: "[$messageCode]$defaultMessage"
        // 生成Result对象
        return Result(messageCode.toInt(), message, data)
    }

    /**
     * 获取code对应的中英文信息
     * @param messageCode code
     * @param checkUrlDecoder 考虑利用URL编码以支持多行信息，以及带特殊字符的信息
     * @return Result响应结果对象
     */
    fun getCodeLanMessage(
        messageCode: String,
        defaultMessage: String? = null,
        params: Array<String>? = null,
        checkUrlDecoder: Boolean = false
    ): String {
        return getCodeMessage(messageCode, params = params)
            ?.let { if (checkUrlDecoder) URLDecoder.decode(it, "UTF-8") else it }
            ?: defaultMessage ?: messageCode
    }

    /**
     * 获取code对应的中英文信息
     * @param messageCode code
     * @param params 替换描述信息占位符的参数数组
     * @return Result响应结果对象
     */
    fun getCodeMessage(messageCode: String, params: Array<String>?): String? {
        var message: String? = null
        try {
            val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
            // 根据code从redis中获取该状态码对应的信息信息(BCI_CODE_PREFIX前缀保证code码在redis中的唯一性)
            val messageCodeDetailStr = redisOperation.get(BCI_CODE_PREFIX + messageCode)
                ?: return message
            val messageCodeDetail =
                JsonUtil.getObjectMapper().readValue(messageCodeDetailStr, MessageCodeDetail::class.java)
            // 根据字符集取出对应的状态码描述信息
            message = getMessageByLocale(messageCodeDetail)
            if (null != params) {
                val mf = MessageFormat(message)
                // 根据参数动态替换状态码描述里的占位符
                message = mf.format(params)
            }
        } catch (ignored: Exception) {
            logger.error("$messageCode get message error is :$ignored", ignored)
        }
        return message
    }

    /**
     * 根据locale信息获取对应的语言描述信息
     * @param messageCodeDetail 返回码详情
     * @return 语言描述信息
     */
    private fun getMessageByLocale(messageCodeDetail: MessageCodeDetail): String {
        return when (CommonUtils.getBkLocale()) {
            CommonUtils.ZH_CN -> messageCodeDetail.messageDetailZhCn // 简体中文描述
            CommonUtils.ZH_TW -> messageCodeDetail.messageDetailZhTw ?: "" // 繁体中文描述
            else -> messageCodeDetail.messageDetailEn ?: "" // 英文描述
        }
    }

    /**
     * 根据locale信息获取对应的语言描述信息
     * @param chinese 中文描述信息
     * @param english 英文描述信息
     * @return 语言描述信息
     */
    fun getMessageByLocale(chinese: String, english: String?): String {
        return when (CommonUtils.getBkLocale()) {
            CommonUtils.ZH_CN -> chinese // 简体中文描述
            CommonUtils.ZH_TW -> chinese // 繁体中文描述
            else -> english ?: "" // 英文描述
        }
    }
}
