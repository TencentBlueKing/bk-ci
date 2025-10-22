/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.service.StoreFileService
import com.tencent.devops.store.common.service.StoreFileService.Companion.BK_CI_PATH_REGEX
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.utils.StoreFileAnalysisUtil.isDirectoryNotEmpty
import com.tencent.devops.store.pojo.common.StoreI18nConfig
import com.tencent.devops.store.pojo.common.TextReferenceFileDownloadRequest
import java.io.File
import java.util.Properties
import java.util.concurrent.Executors
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList")
abstract class StoreI18nMessageServiceImpl : StoreI18nMessageService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var commonConfig: CommonConfig

    @Autowired
    lateinit var storeFileService: StoreFileService

    companion object {
        private const val MESSAGE_NAME_TEMPLATE = "message_%s.properties"
        private const val BATCH_HANDLE_NUM = 50
        private val executors = Executors.newFixedThreadPool(5)
        private val logger = LoggerFactory.getLogger(StoreI18nMessageServiceImpl::class.java)
    }

    override fun parseJsonMapI18nInfo(
        userId: String,
        storeI18nConfig: StoreI18nConfig,
        jsonMap: MutableMap<String, Any>,
        version: String
    ): Map<String, Any> {
        val projectCode = storeI18nConfig.projectCode
        val fileDir = storeI18nConfig.fileDir
        val i18nDir = storeI18nConfig.i18nDir
        val propertiesKeyPrefix = storeI18nConfig.propertiesKeyPrefix
        val dbKeyPrefix = storeI18nConfig.dbKeyPrefix
        val repositoryHashId = storeI18nConfig.repositoryHashId
        logger.info(
            "parseJsonMap params:[$userId|$projectCode|$fileDir|$i18nDir|$propertiesKeyPrefix|$dbKeyPrefix|" +
                    "$repositoryHashId]"
        )
        // 获取蓝盾默认语言信息
        val devopsDefaultLocaleLanguage = commonConfig.devopsDefaultLocaleLanguage
        val jsonLocaleLanguage = jsonMap[KEY_DEFAULT_LOCALE_LANGUAGE] ?: DEFAULT_LOCALE_LANGUAGE
        logger.info("parseJsonMapI18nInfo:[$devopsDefaultLocaleLanguage|$jsonLocaleLanguage]")
        // 获取蓝盾默认语言的资源文件
        val fileName = MESSAGE_NAME_TEMPLATE.format(devopsDefaultLocaleLanguage)
        val defaultProperties = getMessageProperties(
            projectCode = projectCode,
            fileDir = fileDir,
            i18nDir = i18nDir,
            fileName = fileName,
            repositoryHashId = repositoryHashId,
            branch = storeI18nConfig.branch
        )
        val fieldLocaleInfos = if (jsonLocaleLanguage == devopsDefaultLocaleLanguage) {
            // 如果map集合中默认字段值对应的语言和蓝盾默认语言一致，则无需替换
            defaultProperties?.map {
                val fieldName = if (!propertiesKeyPrefix.isNullOrBlank()) {
                    // 如果字段key前缀不为空，需为key加上前缀
                    "$propertiesKeyPrefix.${it.key}"
                } else {
                    it.key.toString()
                }
                FieldLocaleInfo(fieldName, it.value.toString())
            }?.toMutableList()
        } else {
            // 遍历map集合替换国际化字段值为默认语言值并获取国际化字段列表
            defaultProperties?.let {
                MessageUtil.traverseMap(
                    dataMap = jsonMap,
                    keyPrefix = propertiesKeyPrefix,
                    properties = defaultProperties
                )
            }
        }
        // 异步解析处理国际化资源文件信息
        fieldLocaleInfos?.let {
            asyncHandleI18nMessage(
                userId = userId,
                version = version,
                fieldLocaleInfos = fieldLocaleInfos,
                storeI18nConfig = storeI18nConfig
            )
        }
        return jsonMap
    }

    override fun parseErrorCodeI18nInfo(
        userId: String,
        errorCodes: Set<Int>,
        version: String,
        storeI18nConfig: StoreI18nConfig
    ) {
        logger.info("parseErrorCode params:[$userId|${storeI18nConfig.projectCode}" +
                "|${storeI18nConfig.fileDir}|${storeI18nConfig.i18nDir}|${storeI18nConfig.dbKeyPrefix}" +
                "|${storeI18nConfig.repositoryHashId}]")
        val fieldLocaleInfos = mutableListOf<FieldLocaleInfo>()
        errorCodes.forEach { errorCode ->
            fieldLocaleInfos.add(FieldLocaleInfo(fieldName = errorCode.toString(), fieldValue = ""))
        }
        // 异步解析处理国际化资源文件信息
        asyncHandleI18nMessage(
            userId = userId,
            fieldLocaleInfos = fieldLocaleInfos,
            version = version,
            storeI18nConfig = storeI18nConfig
        )
    }

    override fun parseJsonStrI18nInfo(jsonStr: String, keyPrefix: String): String {
        val userId = I18nUtil.getRequestUserId()
        val language = I18nUtil.getLanguage(userId)
        val devopsDefaultLocaleLanguage = commonConfig.devopsDefaultLocaleLanguage
        if (language == devopsDefaultLocaleLanguage) {
            // 如果请求的语言信息和默认语言一致，则无需对json字符串进行国际化替换
            return jsonStr
        }
        // 根据key前缀查出对应的国际化信息
        val i18nMessages = client.get(ServiceI18nMessageResource::class).getI18nMessagesByKeyPrefix(
            keyPrefix = keyPrefix,
            moduleCode = SystemModuleEnum.STORE.name,
            language = I18nUtil.getLanguage(userId)
        ).data
        return if (i18nMessages.isNullOrEmpty()) {
            // 如果查出来的国际化信息为空则无需进行国际化替换
            jsonStr
        } else {
            val jsonMap = try {
                JsonUtil.toMutableMap(jsonStr)
            } catch (ignored: Throwable) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR
                )
            }
            // 把国际化信息放入properties对象中
            val properties = Properties()
            i18nMessages.forEach { i18nMessage ->
                properties[i18nMessage.key] = i18nMessage.value
            }
            // 对jsonMap中的国际化字段就行国际化信息替换
            MessageUtil.traverseMap(jsonMap, keyPrefix, properties)
            JsonUtil.toJson(jsonMap, false)
        }
    }

    private fun asyncHandleI18nMessage(
        userId: String,
        storeI18nConfig: StoreI18nConfig,
        fieldLocaleInfos: MutableList<FieldLocaleInfo>,
        version: String
    ) {
        executors.submit {
            // 获取资源文件名称列表
            val propertiesFileNames = storeFileService.getFileNames(
                projectCode = storeI18nConfig.projectCode,
                fileDir = storeI18nConfig.fileDir,
                i18nDir = storeI18nConfig.i18nDir,
                repositoryHashId = storeI18nConfig.repositoryHashId,
                branch = storeI18nConfig.branch
            )
            logger.info("parseJsonMap propertiesFileNames:$propertiesFileNames")
            val regex = MESSAGE_NAME_TEMPLATE.format("(.*)").toRegex()
            propertiesFileNames?.forEach { propertiesFileName ->
                val matchResult = regex.find(propertiesFileName)
                // 根据资源文件名称获取资源文件的语言信息
                val language = matchResult?.groupValues?.get(1) ?: return@forEach
                val fileProperties = getMessageProperties(
                    projectCode = storeI18nConfig.projectCode,
                    fileDir = storeI18nConfig.fileDir,
                    i18nDir = storeI18nConfig.i18nDir,
                    fileName = propertiesFileName,
                    repositoryHashId = storeI18nConfig.repositoryHashId,
                    branch = storeI18nConfig.branch
                ) ?: return@forEach
                val textReferenceContentMap = getTextReferenceFileContent(
                    projectCode = storeI18nConfig.projectCode,
                    properties = fileProperties,
                    repositoryHashId = storeI18nConfig.repositoryHashId,
                    fileDir = storeI18nConfig.fileDir,
                    branch = storeI18nConfig.branch
                )
                var textReferenceFileDirPath: String? = null
                val allFileNames = textReferenceContentMap.values.flatten().toSet()
                if (textReferenceContentMap.isNotEmpty()) {
                    textReferenceFileDirPath = storeFileService.getTextReferenceFileDir(
                        userId = userId,
                        version = version,
                        request = TextReferenceFileDownloadRequest(
                            projectCode = storeI18nConfig.projectCode,
                            fileDir = storeI18nConfig.fileDir,
                            repositoryHashId = storeI18nConfig.repositoryHashId,
                            storeCode = storeI18nConfig.storeCode,
                            fileNames = allFileNames
                        )
                    )
                }
                val isDirectoryNotEmpty = isDirectoryNotEmpty(textReferenceFileDirPath)
                if (isDirectoryNotEmpty) {
                    textReferenceContentMap.forEach { (key, _) ->
                        fileProperties[key] = getTextReferenceFileParsing(
                            userId = userId,
                            fileDir = textReferenceFileDirPath!!,
                            content = fileProperties[key] as String
                        )
                    }
                }

                val i18nMessages = generateI18nMessages(
                    fieldLocaleInfos = fieldLocaleInfos,
                    fileProperties = fileProperties,
                    language = language,
                    dbKeyPrefix = storeI18nConfig.dbKeyPrefix
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
        dbKeyPrefix: String? = null
    ): MutableList<I18nMessage> {
        val i18nMessages = mutableListOf<I18nMessage>()
        fieldLocaleInfos.forEach { fieldLocaleInfo ->
            val fieldName = fieldLocaleInfo.fieldName
            val fieldI18nValue = fileProperties[fieldName]
            fieldI18nValue?.let {
                // 国际化资源文件中有该字段的值则把该字段信息加入国际化信息集合中
                val key = if (!dbKeyPrefix.isNullOrBlank()) {
                    // 如果字段key前缀不为空，需为key加上前缀
                    "$dbKeyPrefix.$fieldName"
                } else {
                    fieldName
                }
                i18nMessages.add(
                    I18nMessage(
                        moduleCode = SystemModuleEnum.STORE.name,
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
        i18nDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String? = null
    ): Properties? {
        val fileStr = getFileStr(
            projectCode = projectCode,
            fileDir = fileDir,
            fileName = "$i18nDir/$fileName",
            repositoryHashId = repositoryHashId,
            branch = branch
        )
        return if (fileStr.isNullOrBlank()) {
            null
        } else {
            MessageUtil.getMessageProperties(fileStr)
        }
    }

    abstract fun getFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): String?

    /**
     * 获取存在文件引用的配置
     */
    fun getTextReferenceFileContent(
        projectCode: String,
        properties: Properties,
        fileDir: String,
        repositoryHashId: String? = null,
        branch: String? = null
    ): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()

        properties.keys.map { it as String }.forEach { key ->
            val text = properties[key].toString()
            val fileNames = getFilePathAnalysis(text)
            val mdFileNames = fileNames.filter { it.endsWith(".md") }
            if (mdFileNames.isEmpty()) {
                map[key] = fileNames
            } else {
                val fileStr = getFileStr(
                    projectCode = projectCode,
                    fileDir = fileDir,
                    fileName = "file${File.separator}${mdFileNames[0]}",
                    repositoryHashId = repositoryHashId,
                    branch = branch
                )
                fileStr?.let {
                    map[key] = getFilePathAnalysis(fileStr)
                    properties[key] = fileStr
                }
            }
        }
        return map
    }

    fun getFilePathAnalysis(
        text: String
    ): MutableList<String> {
        val regex = Regex(pattern = BK_CI_PATH_REGEX)
        val matchResult = regex.findAll(text)
        val fileNames = mutableListOf<String>()
        matchResult.forEach {
            val fileName = it.groupValues[2].replace("\"", "")
            fileNames.add(fileName)
        }
        return fileNames
    }

    private fun getTextReferenceFileParsing(
        userId: String,
        fileDir: String,
        content: String,
        recursionFlag: Boolean = false
    ): String {
        var result = content
        val fileNames: MutableList<String> = mutableListOf()
        val regex = Regex(pattern = BK_CI_PATH_REGEX)
        val matchResult = regex.findAll(content)
        try {
            matchResult.forEach {
                val fileName = it.groupValues[2].replace("\"", "")
                val isStatic = storeFileService.isStaticFile(fileName)
                // 文本文件引用只允许引用一层，防止循环引用
                val textFile = File("$fileDir${File.separator}$fileName")
                if (!isStatic && !recursionFlag && textFile.exists()) {
                    return getTextReferenceFileParsing(
                        userId = userId,
                        fileDir = fileDir,
                        content = textFile.readText(),
                        recursionFlag = true
                    )
                }
                if (isStatic) {
                    fileNames.add(fileName)
                }
            }
            if (fileNames.isNotEmpty()) {
                result = storeFileService.getStaticFileReference(
                    userId = userId,
                    content = content,
                    fileDirPath = fileDir,
                    fileNames = fileNames
                )
            }
        } catch (ignored: Throwable) {
            logger.warn("failed to parse text reference message:${ignored.message}")
        }
        return result
    }
}
