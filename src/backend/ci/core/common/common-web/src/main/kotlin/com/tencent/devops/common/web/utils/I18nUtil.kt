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

import com.tencent.devops.common.api.constant.REQUEST_CHANNEL
import com.tencent.devops.common.api.enums.RequestChannelTypeEnum
import com.tencent.devops.common.api.util.LocaleUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.ServiceLocaleResource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object I18nUtil {


    /**
     * 从redis缓存获取用户的国际化语言信息
     * @param userId 用户ID
     * @return 语言信息
     */
    fun getUserLocaleLanguageFromCache(userId: String): String? {
        val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
        return redisOperation.get(LocaleUtil.getUserLocaleLanguageKey(userId))
    }

    /**
     * 获取蓝盾默认支持的语言
     * @return 系统默认语言
     */
    fun getDefaultLocaleLanguage(): String {
        val commonConfig: CommonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return commonConfig.devopsDefaultLocaleLanguage
    }

    /**
     * 获取接口请求渠道信息
     * @return 渠道信息
     */
    fun getRequestChannel(): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return if (null != attributes) {
            val request = attributes.request
            (request.getAttribute(REQUEST_CHANNEL) ?: request.getHeader(REQUEST_CHANNEL))?.toString()
        } else {
            null // 不是接口请求来源则返回null
        }
    }

    fun getLanguage (userId: String): String{
        if (getRequestChannel() == RequestChannelTypeEnum.USER.name) {
            if (getUserLocaleLanguageFromCache(userId).isNullOrBlank()){
                val client = SpringContextUtil.getBean(Client::class.java)
                return client.get(ServiceLocaleResource::class).getUserLocale(userId).data!!.language
            }
            return getUserLocaleLanguageFromCache(userId).toString()
        }else{
            return getDefaultLocaleLanguage()
        }
    }
}