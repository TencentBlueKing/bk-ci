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

package com.tencent.devops.store.atom.service.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.AND
import com.tencent.devops.common.api.constant.DANG
import com.tencent.devops.common.api.constant.DEFAULT
import com.tencent.devops.common.api.constant.MULTIPLE_SELECTOR
import com.tencent.devops.common.api.constant.NO_LABEL
import com.tencent.devops.common.api.constant.OPTIONS
import com.tencent.devops.common.api.constant.OR
import com.tencent.devops.common.api.constant.REQUIRED
import com.tencent.devops.common.api.constant.SINGLE_SELECTOR
import com.tencent.devops.common.api.constant.TIMETOSELECT
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.atom.dao.MarketAtomFeatureDao
import com.tencent.devops.store.atom.service.AtomYamlGenerateService
import com.tencent.devops.store.common.service.StoreI18nMessageService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode.TASK_JSON_CONFIGURE_FORMAT_ERROR
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomYamlGenerateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomDao: MarketAtomDao,
    private val marketAtomFeatureDao: MarketAtomFeatureDao,
    private val storeI18nMessageService: StoreI18nMessageService
) : AtomYamlGenerateService {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomYamlGenerateServiceImpl::class.java)
        private val MULTI_VALUE_TYPES = setOf("atom-checkbox-list", "staff-input", "company-staff-input", "parameter")
    }

    override fun generateCiYaml(
        atomCode: String?,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): String {
        val atomCodeList = if (atomCode.isNullOrBlank()) {
            marketAtomDao.getSupportGitCiAtom(dslContext, os, classType).map { it.value1() }
        } else {
            listOf(atomCode)
        }

        return buildString {
            atomCodeList.forEach {
                val atom = marketAtomDao.getLatestAtomByCode(dslContext, it) ?: return@forEach
                val feature = marketAtomFeatureDao.getAtomFeature(dslContext, it) ?: return@forEach
                if (feature.recommendFlag == null || feature.recommendFlag) {
                    append(generateYaml(atom, defaultShowFlag))
                    append("\r\n\r\n")
                }
            }
        }
    }

    override fun generateCiV2Yaml(
        atomCode: String,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): String {
        val atom = marketAtomDao.getLatestAtomByCode(dslContext, atomCode) ?: return ""
        val feature = marketAtomFeatureDao.getAtomFeature(dslContext, atomCode) ?: return ""
        return if (feature.recommendFlag == null || feature.recommendFlag) {
            generateV2Yaml(atom, defaultShowFlag)
        } else {
            ""
        }
    }

    private data class AtomParamMeta(
        val description: String,
        val type: Any?,
        val required: Boolean,
        val defaultValue: Any?,
        val isMultiValue: Boolean
    )

    @Suppress("UNCHECKED_CAST")
    private fun parseAtomInput(atom: TAtomRecord): Pair<Map<String, Any>?, Map<String, Any>?> {
        val propJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = atom.props,
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atom.atomCode, atom.version)
        )
        val props: Map<String, Any> = jacksonObjectMapper().readValue(propJsonStr)
        return Pair(props["input"] as? Map<String, Any>, props["output"] as? Map<String, Any>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractParamMeta(paramValueMap: Map<String, Any>, fallbackNoLabel: Boolean = false): AtomParamMeta {
        val label = paramValueMap["label"]?.toString()?.takeIf { it.isNotBlank() }
        val text = paramValueMap["text"]?.toString()?.takeIf { it.isNotBlank() }
        val desc = paramValueMap["desc"]?.toString()?.takeIf { it.isNotBlank() }
        val description = if (fallbackNoLabel) {
            label ?: text ?: desc ?: I18nUtil.getCodeLanMessage(NO_LABEL)
        } else {
            label ?: text ?: desc ?: ""
        }

        val type = paramValueMap["type"]
        val required = "true".equals(paramValueMap["required"]?.toString(), ignoreCase = true)
        val defaultValue = paramValueMap["default"]

        val optionsConf = paramValueMap["optionsConf"] as? Map<String, Any>
        val isMultiple = optionsConf?.get("multiple")
            ?.let { "true".equals(it.toString(), ignoreCase = true) } ?: false
        val isMultiValue = (type == "selector" && isMultiple) || type in MULTI_VALUE_TYPES

        return AtomParamMeta(
            description = description,
            type = type,
            required = required,
            defaultValue = defaultValue,
            isMultiValue = isMultiValue
        )
    }

    private fun StringBuilder.appendRequiredAndDefault(
        required: Boolean,
        defaultValue: Any?,
        requiredName: String,
        defaultName: String
    ) {
        if (required) append(", $requiredName")
        if (defaultValue != null && defaultValue.toString().isNotBlank()) {
            append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateYaml(atom: TAtomRecord, defaultShowFlag: Boolean?): String {
        return buildString {
            if (defaultShowFlag == true) {
                append("h2. ${atom.name}\r\n")
                append("{code:theme=Midnight|linenumbers=true|language=YAML|collapse=false}\r\n")
            }
            append("- taskType: marketBuild@latest\r\n")
            append("  displayName: ${atom.name}\r\n")
            append("  inputs:\r\n")
            append("    atomCode: ${atom.atomCode}\r\n")
            append("    name: ${atom.name}\r\n")
            append("    version: ${atom.version}\r\n")
            append("    data:\r\n")
            append("      input:\r\n")

            val (input, output) = parseAtomInput(atom)
            val requiredName = I18nUtil.getCodeLanMessage(messageCode = REQUIRED)
            val defaultName = I18nUtil.getCodeLanMessage(messageCode = DEFAULT)

            input?.forEach { (paramKey, paramValue) ->
                val paramValueMap = paramValue as Map<String, Any>
                val meta = extractParamMeta(paramValueMap)

                if (meta.isMultiValue) {
                    append("        $paramKey: ")
                    append("\t\t# ${meta.description}")
                    appendRequiredAndDefault(meta.required, meta.defaultValue, requiredName, defaultName)
                    append("\r\n")
                    append("        - string\r\n")
                    append("        - string\r\n")
                } else {
                    append("        $paramKey: ")
                    append(if (meta.type == "atom-checkbox") "boolean" else "string")
                    append("\t\t# ${meta.description.toString().replace("\n", "")}")
                    appendRequiredAndDefault(meta.required, meta.defaultValue, requiredName, defaultName)
                    append("\r\n")
                }
            }

            if (output != null) {
                append("      output: \r\n")
                output.forEach { append("        ${it.key}: string \r\n") }
            } else {
                append("      output: {}\r\n")
            }
            if (defaultShowFlag == true) {
                append("{code}\r\n \r\n")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateV2Yaml(atom: TAtomRecord, defaultShowFlag: Boolean?): String {
        val userId = I18nUtil.getRequestUserId()
        return buildString {
            if (defaultShowFlag == true) {
                append("h2. ${atom.name}\r\n")
                append("{code:theme=Midnight|linenumbers=true|language=YAML|collapse=false}\r\n")
            }
            val latestVersion = "${atom.version.split('.').first()}.*"
            append("- uses: ${atom.atomCode}@$latestVersion\r\n")
            append("  name: ${atom.name}\r\n")

            val (input, _) = parseAtomInput(atom)
            if (input != null) {
                append("  with:\r\n")
                val requiredName = I18nUtil.getCodeLanMessage(REQUIRED)
                val defaultName = I18nUtil.getCodeLanMessage(DEFAULT)
                val optionsName = I18nUtil.getCodeLanMessage(OPTIONS)
                val multipleName = I18nUtil.getCodeLanMessage(MULTIPLE_SELECTOR)
                val singleName = I18nUtil.getCodeLanMessage(SINGLE_SELECTOR)

                input.forEach { (paramKey, paramValue) ->
                    val paramValueMap = paramValue as Map<String, Any>
                    val meta = extractParamMeta(paramValueMap, fallbackNoLabel = true)
                    try {
                        val selectorTypeName = if (meta.isMultiValue) multipleName else singleName
                        appendParamComment(
                            meta = meta,
                            paramValueMap = paramValueMap,
                            optionsName = optionsName,
                            selectorTypeName = selectorTypeName,
                            requiredName = requiredName,
                            defaultName = defaultName
                        )
                        append("\r\n")
                        if (meta.isMultiValue) {
                            append("    $paramKey:\r\n")
                            append("        - string\r\n")
                            append("        - string\r\n")
                        } else {
                            append("    $paramKey: ")
                            when (meta.type) {
                                "atom-checkbox" -> append("boolean")
                                "key-value-normal" -> append(
                                    "\n    - key: string\n      value: string"
                                )
                                else -> append("string")
                            }
                            append("\r\n")
                        }
                    } catch (ignored: Throwable) {
                        insert(
                            0,
                            MessageUtil.getMessageByLocale(
                                TASK_JSON_CONFIGURE_FORMAT_ERROR,
                                I18nUtil.getLanguage(userId),
                                arrayOf(paramKey, "${ignored.message}")
                            )
                        )
                    }
                }
            }

            if (defaultShowFlag == true) {
                append("{code}\r\n \r\n")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun StringBuilder.appendParamComment(
        meta: AtomParamMeta,
        paramValueMap: Map<String, Any>,
        optionsName: String,
        selectorTypeName: String,
        requiredName: String,
        defaultName: String
    ) {
        append("    # ${meta.description.replace("\n", "")}")
        appendRequiredAndDefault(meta.required, meta.defaultValue, requiredName, defaultName)

        (paramValueMap["rely"] as? Map<String, Any>)?.let { appendRelyInfo(it) }

        (paramValueMap["options"] as? List<Map<String, Any>>)?.let { options ->
            append(", $selectorTypeName, $optionsName:")
            appendOptionItems(options) { "${it["id"]}[${it["name"]}]" }
        }

        (paramValueMap["list"] as? List<Map<String, Any>>)?.let { list ->
            append(", $optionsName:")
            appendOptionItems(list) { map ->
                val key = map["label"] ?: map["id"] ?: return@appendOptionItems null
                val value = map["value"] ?: map["name"] ?: return@appendOptionItems null
                "$key[$value]"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun StringBuilder.appendRelyInfo(rely: Map<String, Any>) {
        try {
            val expression = rely["expression"] as? List<Map<String, Any>> ?: return
            val dang = I18nUtil.getCodeLanMessage(messageCode = DANG)
            val and = I18nUtil.getCodeLanMessage(messageCode = AND)
            val or = I18nUtil.getCodeLanMessage(messageCode = OR)
            val timeToSelect = I18nUtil.getCodeLanMessage(messageCode = TIMETOSELECT)
            val link = if (rely["operation"] == "AND") and else or

            append(", $dang")
            expression.forEachIndexed { index, exp ->
                append(" [${exp["key"]}] = [${exp["value"]}] ")
                if (index < expression.size - 1) append(link)
            }
            append(timeToSelect)
        } catch (ignored: Throwable) {
            logger.warn("load atom input[rely] with error", ignored)
        }
    }

    private fun StringBuilder.appendOptionItems(
        items: List<Map<String, Any>>,
        formatter: (Map<String, Any>) -> String?
    ) {
        try {
            items.forEachIndexed { index, map ->
                val text = formatter(map) ?: return
                append(" $text")
                if (index < items.size - 1) append(" |")
            }
        } catch (ignored: Throwable) {
            logger.warn("load atom input[options/list] with error", ignored)
        }
    }
}
