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
package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.KEY_DEFAULT_LOCALE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.FieldLocaleInfo
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.service.common.StoreI18nMessageService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
abstract class StoreI18nMessageServiceImpl : StoreI18nMessageService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var commonConfig: CommonConfig

    override fun parseJsonMap(
        userId: String,
        projectCode: String,
        jsonMap: MutableMap<String, Any>,
        filePathPrefix: String,
        keyPrefix: String?,
        repositoryHashId: String?
    ): Map<String, Any> {
        // 获取蓝盾默认语言信息
        val devopsDefaultLocale = commonConfig.devopsDefaultLocale
        val jsonLocale = jsonMap[KEY_DEFAULT_LOCALE]
        if (jsonLocale == devopsDefaultLocale) {
            // 如果map集合中默认字段值对应的语言和蓝盾默认语言一致，则无需替换
            return jsonMap
        } else {
            // 获取蓝盾默认语言的资源文件
            val fileName = "message_$devopsDefaultLocale.properties"
            val fileStr = getPropertiesFileStr(
                projectCode = projectCode,
                filePathPrefix = filePathPrefix,
                fileName = fileName,
                repositoryHashId = repositoryHashId
            )
            if (fileStr.isNullOrBlank()) {
                // 如果用户的组件未提供系统默认语言的资源文件，则抛出错误提示
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_DEFAULT_LOCALE_FILE_NOT_EXIST,
                    params = arrayOf(devopsDefaultLocale)
                )
            }
            val properties = MessageUtil.getMessageProperties(fileStr)
            // 遍历map集合获取字段列表
            val fieldLocaleInfoList = MessageUtil.traverseMap(jsonMap, keyPrefix, properties)
        }
        return jsonMap
    }

    abstract fun getPropertiesFileStr(
        projectCode: String,
        filePathPrefix: String,
        fileName: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): String?
}
