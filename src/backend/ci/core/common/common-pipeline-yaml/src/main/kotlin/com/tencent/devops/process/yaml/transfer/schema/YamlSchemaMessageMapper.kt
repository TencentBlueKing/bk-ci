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

package com.tencent.devops.process.yaml.transfer.schema

import com.networknt.schema.ValidationMessage
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.process.yaml.transfer.PipelineTransferValidateDetail

/**
 * 将 JSON Schema 校验器（networknt）的原始报错转换为友好化描述，供各条 YAML 校验链路复用，
 * 避免同一份 schema 在不同链路上出现"一处本地化、一处全英文"的体验差异。
 *
 * - PAC/通用链路：[CodeSchemaCheck] 将明细放进 PipelineTransferException，由前端按 messageCode 渲染；
 * - 工蜂 CI（Stream）链路：用 [toLocalizedMessage] 直接渲染成本地化文案后随自身异常抛出。
 */
object YamlSchemaMessageMapper {

    /** 默认语言，与 MessageUtil 的兜底保持一致 */
    private const val DEFAULT_LANGUAGE = "zh_CN"

    /**
     * 单条原始报错 -> 友好化明细（messageCode + params）
     */
    fun map(msg: ValidationMessage): PipelineTransferValidateDetail {
        val path = msg.instanceLocation?.toString()?.ifBlank { "$" } ?: "$"
        val firstArg = msg.arguments?.firstOrNull()?.toString().orEmpty()
        return when (msg.type) {
            "required" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_REQUIRED,
                params = listOf(path, firstArg)
            )
            "additionalProperties" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_UNSUPPORTED_FIELD,
                params = listOf(path, firstArg)
            )
            "not" -> {
                val forbidden = extractForbiddenFields(msg.message)
                if (forbidden.isNotBlank()) {
                    PipelineTransferValidateDetail(
                        messageCode = CommonMessageCode.YAML_SCHEMA_UNSUPPORTED_FIELD,
                        params = listOf(path, forbidden)
                    )
                } else {
                    PipelineTransferValidateDetail(
                        messageCode = CommonMessageCode.YAML_SCHEMA_UNSUPPORTED_CONFIG,
                        params = listOf(path)
                    )
                }
            }
            "enum" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_ENUM,
                params = listOf(path, msg.arguments?.joinToString(", ").orEmpty())
            )
            "type" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_TYPE,
                params = listOf(path, firstArg)
            )
            "pattern" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_PATTERN,
                params = listOf(path, firstArg)
            )
            "minLength", "maxLength" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_LENGTH,
                params = listOf(path, msg.type, firstArg)
            )
            "minimum", "maximum" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_RANGE,
                params = listOf(path, msg.type, firstArg)
            )
            // oneOf/anyOf 的原始报错是英文且会把各分支错误混在一起，单独给一条友好提示；
            // 各分支的具体原因（类型/格式等）已作为同组错误一并列出
            "oneOf", "anyOf" -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_ONE_OF,
                params = listOf(path)
            )
            else -> PipelineTransferValidateDetail(
                messageCode = CommonMessageCode.YAML_SCHEMA_UNSUPPORTED_CONFIG,
                params = listOf("$path ${msg.message}")
            )
        }
    }

    /**
     * 批量转换：用于可携带明细的异常（如 PipelineTransferException）
     */
    fun toDetails(messages: Set<ValidationMessage>): List<PipelineTransferValidateDetail> =
        messages.map { map(it) }

    /**
     * 渲染成本地化文案：用于不支持 [PipelineTransferValidateDetail] 的链路
     * （如工蜂 CI 的 YamlFormatException，只能携带一段字符串）。
     *
     * 渲染失败时退回原始报错，保证不丢信息。
     */
    fun toLocalizedMessage(
        messages: Set<ValidationMessage>,
        language: String? = null,
        delimiter: String = "; "
    ): String {
        if (messages.isEmpty()) return ""
        val lang = language?.ifBlank { null } ?: DEFAULT_LANGUAGE
        return messages.joinToString(delimiter) { msg ->
            val detail = map(msg)
            val text = MessageUtil.getMessageByLocale(
                messageCode = detail.messageCode,
                language = lang,
                params = detail.params?.toTypedArray()
            )
            text.ifBlank { "${detail.params?.firstOrNull() ?: ""} ${msg.message}".trim() }
        }
    }

    /**
     * Try to extract the forbidden field list from the raw message of the `not` keyword.
     * Typical form: `must not be valid to the schema "xxx" : {"required":["variables"]}`
     */
    private fun extractForbiddenFields(message: String?): String {
        if (message.isNullOrBlank()) return ""
        val regex = """"required"\s*:\s*\[([^]]*)]""".toRegex()
        return regex.find(message)?.groupValues?.get(1)?.replace("\"", "").orEmpty()
    }
}
