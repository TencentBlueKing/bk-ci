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
import com.tencent.devops.store.pojo.common.TextReferenceFileDownloadRequest
import com.tencent.devops.store.service.common.StoreFileService
import com.tencent.devops.store.service.common.StoreFileService.Companion.BK_CI_PATH_REGEX
import com.tencent.devops.store.service.common.StoreI18nMessageService
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.collections4.ListUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
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
        projectCode: String,
        jsonMap: MutableMap<String, Any>,
        fileDir: String,
        i18nDir: String,
        propertiesKeyPrefix: String?,
        dbKeyPrefix: String?,
        repositoryHashId: String?,
        branch: String?,
        version: String
    ): Map<String, Any> {
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
            branch = branch
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
                projectCode = projectCode,
                fileDir = fileDir,
                i18nDir = i18nDir,
                repositoryHashId = repositoryHashId,
                fieldLocaleInfos = fieldLocaleInfos,
                dbKeyPrefix = dbKeyPrefix,
                userId = userId,
                branch = branch,
                version = version
            )
        }
        return jsonMap
    }

    override fun parseErrorCodeI18nInfo(
        userId: String,
        projectCode: String,
        errorCodes: Set<Int>,
        fileDir: String,
        i18nDir: String,
        keyPrefix: String?,
        repositoryHashId: String?,
        branch: String?,
        version: String
    ) {
        logger.info(
            "parseErrorCode params:[$userId|$projectCode|$fileDir|$i18nDir|$keyPrefix|$repositoryHashId|$branch]"
        )
        val fieldLocaleInfos = mutableListOf<FieldLocaleInfo>()
        errorCodes.forEach { errorCode ->
            fieldLocaleInfos.add(FieldLocaleInfo(fieldName = errorCode.toString(), fieldValue = ""))
        }
        // 异步解析处理国际化资源文件信息
        asyncHandleI18nMessage(
            projectCode = projectCode,
            fileDir = fileDir,
            i18nDir = i18nDir,
            repositoryHashId = repositoryHashId,
            fieldLocaleInfos = fieldLocaleInfos,
            dbKeyPrefix = keyPrefix,
            userId = userId,
            branch = branch,
            version = version
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
        projectCode: String,
        fileDir: String,
        i18nDir: String,
        repositoryHashId: String?,
        fieldLocaleInfos: MutableList<FieldLocaleInfo>,
        dbKeyPrefix: String?,
        userId: String,
        branch: String?,
        version: String
    ) {
        executors.submit {
            // 获取资源文件名称列表
            val propertiesFileNames = storeFileService.getFileNames(
                projectCode = projectCode,
                fileDir = fileDir,
                i18nDir = i18nDir,
                repositoryHashId = repositoryHashId,
                branch = branch
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
                    i18nDir = i18nDir,
                    fileName = propertiesFileName,
                    repositoryHashId = repositoryHashId,
                    branch = branch
                ) ?: return@forEach
                val textReferenceContentMap = getTextReferenceFileContent(fileProperties)
                var textReferenceFileDirPath: String? = null
                if (textReferenceContentMap.isNotEmpty()) {
                    textReferenceFileDirPath = storeFileService.getTextReferenceFileDir(
                        userId = userId,
                        version = version,
                        request = TextReferenceFileDownloadRequest(
                            projectCode = projectCode,
                            fileDir = fileDir,
                            repositoryHashId = repositoryHashId
                        )
                    )
                }
                val isDirectoryNotEmpty = isDirectoryNotEmpty(textReferenceFileDirPath)
                if (isDirectoryNotEmpty) {
                    try {
                        textReferenceContentMap.forEach { (k, v) ->
                            fileProperties[k] = getTextReferenceFileParsing(
                                userId = userId,
                                fileDir = textReferenceFileDirPath!!,
                                content = v
                            )
                        }
                    } finally {
                        File(textReferenceFileDirPath!!).deleteRecursively()
                    }
                }

                val i18nMessages = generateI18nMessages(
                    fieldLocaleInfos = fieldLocaleInfos,
                    fileProperties = fileProperties,
                    language = language,
                    dbKeyPrefix = dbKeyPrefix
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
    fun getTextReferenceFileContent(properties: Properties): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
    properties.keys.map { it as String }.forEach { key ->
        val text = properties[key].toString()
        val matcher: Matcher = pattern.matcher(text)
        if (matcher.find()) {
            map[key] = text
        }
    }
        return map
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
                if (!isStatic && !recursionFlag) {
                    val textFile = File("$fileDir${File.separator}$fileName")
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
            logger.info("failed to parse text reference")
        }
        return result
    }

    private fun isDirectoryNotEmpty(path: String?): Boolean {
        if (path == null) {
            return false
        }
        val directory = Paths.get(path)
        return Files.isDirectory(directory) && Files.list(directory).findFirst().isPresent
    }
}
