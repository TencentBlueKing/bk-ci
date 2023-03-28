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

import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.KEY_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.FieldLocaleInfo
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.service.ServiceI18nMessageResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.service.common.StoreI18nMessageService
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.Properties

@Service
@Suppress("LongParameterList")
abstract class StoreI18nMessageServiceImpl : StoreI18nMessageService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var commonConfig: CommonConfig

    companion object {
        private const val MESSAGE_NAME_TEMPLATE = "message_%s.properties"
        private const val BATCH_HANDLE_NUM = 50
        private val executors = Executors.newFixedThreadPool(5)
        private val logger = LoggerFactory.getLogger(StoreI18nMessageServiceImpl::class.java)
    }

    override fun parseJsonMapI18nInfo(
        userId: String,
        projectCode: String,
        jsonMap: MutableMap<String, Any>,
        fileDir: String,
        keyPrefix: String?,
        repositoryHashId: String?
    ): Map<String, Any> {
        logger.info("parseJsonMap params:[$userId|$projectCode|$fileDir|$keyPrefix|$repositoryHashId]")
        // 获取蓝盾默认语言信息
        val devopsDefaultLocaleLanguage = commonConfig.devopsDefaultLocaleLanguage
        val jsonLocaleLanguage = jsonMap[KEY_DEFAULT_LOCALE_LANGUAGE] ?: DEFAULT_LOCALE_LANGUAGE
        if (jsonLocaleLanguage == devopsDefaultLocaleLanguage) {
            // 如果map集合中默认字段值对应的语言和蓝盾默认语言一致，则无需替换
            return jsonMap
        } else {
            // 获取蓝盾默认语言的资源文件
            val fileName = MESSAGE_NAME_TEMPLATE.format(devopsDefaultLocaleLanguage)
            val defaultProperties = getMessageProperties(
                projectCode = projectCode,
                fileDir = fileDir,
                fileName = fileName,
                repositoryHashId = repositoryHashId,
                language = devopsDefaultLocaleLanguage
            )
            // 遍历map集合获取字段列表
            val fieldLocaleInfos = MessageUtil.traverseMap(
                dataMap = jsonMap,
                properties = defaultProperties
            )
            // 异步解析处理国际化资源文件信息
            asyncHandleI18nMessage(
                projectCode = projectCode,
                fileDir = fileDir,
                repositoryHashId = repositoryHashId,
                fieldLocaleInfos = fieldLocaleInfos,
                keyPrefix = keyPrefix,
                userId = userId
            )
        }
        return jsonMap
    }

    override fun parseErrorCodeI18nInfo(
        userId: String,
        projectCode: String,
        errorCodes: Set<Int>,
        fileDir: String,
        keyPrefix: String?,
        repositoryHashId: String?
    ) {
        logger.info("parseErrorCode params:[$userId|$projectCode|$fileDir|$keyPrefix|$repositoryHashId]")
        val fieldLocaleInfos = mutableListOf<FieldLocaleInfo>()
        errorCodes.forEach { errorCode ->
            fieldLocaleInfos.add(FieldLocaleInfo(fieldName = errorCode.toString(), fieldValue = ""))
        }
        // 异步解析处理国际化资源文件信息
        asyncHandleI18nMessage(
            projectCode = projectCode,
            fileDir = fileDir,
            repositoryHashId = repositoryHashId,
            fieldLocaleInfos = fieldLocaleInfos,
            keyPrefix = keyPrefix,
            userId = userId
        )
    }

    override fun parseJsonStrI18nInfo(jsonStr: String, keyPrefix: String) {
        val jsonMap = JsonUtil.toMutableMap(jsonStr)
    }

    private fun asyncHandleI18nMessage(
        projectCode: String,
        fileDir: String,
        repositoryHashId: String?,
        fieldLocaleInfos: MutableList<FieldLocaleInfo>,
        keyPrefix: String?,
        userId: String
    ) {
        executors.submit {
            // 获取资源文件名称列表
            val propertiesFileNames = getPropertiesFileNames(
                projectCode = projectCode,
                fileDir = fileDir,
                repositoryHashId = repositoryHashId
            )
            logger.info("parseJsonMap propertiesFileNames:$propertiesFileNames")
            val regex = MESSAGE_NAME_TEMPLATE.format("(.*)").toRegex()
            propertiesFileNames?.forEach { propertiesFileName ->
                val matchResult = regex.find(propertiesFileName)
                // 根据资源文件名称获取资源文件的语言信息
                val language = matchResult?.groupValues?.get(1) ?: return@forEach
                val fileProperties = getMessageProperties(
                    projectCode = projectCode,
                    fileDir = fileDir,
                    fileName = propertiesFileName,
                    repositoryHashId = repositoryHashId,
                    language = language
                )
                val i18nMessages = generateI18nMessages(
                    fieldLocaleInfos = fieldLocaleInfos,
                    fileProperties = fileProperties,
                    language = language,
                    keyPrefix = keyPrefix
                )
                // 按批次保存字段的国际化信息
                ListUtils.partition(i18nMessages, BATCH_HANDLE_NUM).forEach { partitionMessages ->
                    client.get(ServiceI18nMessageResource::class).batchAddI18nMessage(userId, partitionMessages)
                }
            }
        }
    }

    private fun generateI18nMessages(
        fieldLocaleInfos: MutableList<FieldLocaleInfo>,
        fileProperties: Properties,
        language: String,
        keyPrefix: String? = null
    ): MutableList<I18nMessage> {
        val i18nMessages = mutableListOf<I18nMessage>()
        fieldLocaleInfos.forEach { fieldLocaleInfo ->
            val fieldName = fieldLocaleInfo.fieldName
            val fieldI18nValue = fileProperties[fieldName]
            fieldI18nValue?.let {
                // 国际化资源文件中有该字段的值则把该字段信息加入国际化信息集合中
                val key = if (!keyPrefix.isNullOrBlank()) {
                    // 如果字段key前缀不为空，需为key加上前缀
                    "$keyPrefix.$fieldName"
                } else {
                    fieldName
                }
                i18nMessages.add(
                    I18nMessage(
                        moduleCode = SystemModuleEnum.STORE,
                        language = language,
                        key = key,
                        value = it.toString()
                    )
                )
            }
        }
        return i18nMessages
    }

    private fun getMessageProperties(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String?,
        language: String
    ): Properties {
        val fileStr = getPropertiesFileStr(
            projectCode = projectCode,
            fileDir = fileDir,
            fileName = fileName,
            repositoryHashId = repositoryHashId
        )
        if (fileStr.isNullOrBlank()) {
            // 如果用户的组件未提供系统默认语言的资源文件，则抛出错误提示
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_LOCALE_FILE_NOT_EXIST,
                params = arrayOf(language)
            )
        }
        return MessageUtil.getMessageProperties(fileStr)
    }

    abstract fun getPropertiesFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): String?

    abstract fun getPropertiesFileNames(
        projectCode: String,
        fileDir: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): List<String>?
}
