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

package com.tencent.devops.store.common.utils

import com.tencent.devops.store.pojo.common.contract.StoreComponentContractCoverageEnum

object StoreComponentContractUtils {

    private val VERSION_REGEX = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val CARD_RISK_SEVERITIES = setOf("high", "medium")
    private val CARD_RISK_FIELDS = listOf("id", "severity", "title", "trigger", "guidance")
    private val CARD_CONSTRAINT_FIELDS = listOf("id", "severity", "rule", "when", "check", "error_code")
    private val CARD_OUTPUT_FIELDS = listOf("name", "type", "set_when", "note")
    private val CARD_PLAIN_SECTIONS = listOf(
        "contract_schema_version",
        "atom_code",
        "based_on_version",
        "coverage",
        "summary",
        "not_for",
        "declared_but_never_set"
    )

    enum class WriteAction {
        CREATE, // 新建
        OVERWRITE, // 覆盖
        MARK_PENDING_COMPARE, // 不覆盖已核验契约，置过期待人工对比
        SKIP_VERIFIED, // 已核验契约与请求一致，不写入
        SKIP_OUTDATED // 库中契约基于更新的版本，不写入
    }

    data class ContractState(
        val basedOnVersion: String,
        val sourceCommit: String?,
        val propsHash: String,
        val coverage: String,
        val lastSeenVersion: String?
    )

    fun isValidVersion(version: String) = VERSION_REGEX.matches(version)

    fun decideWrite(
        existing: ContractState?,
        basedOnVersion: String,
        sourceCommit: String?,
        propsHash: String
    ): WriteAction {
        if (existing == null) {
            return WriteAction.CREATE
        }
        if (isValidVersion(existing.basedOnVersion) &&
            StoreUtils.isGreaterVersion(existing.basedOnVersion, basedOnVersion)
        ) {
            return WriteAction.SKIP_OUTDATED
        }
        if (existing.coverage == StoreComponentContractCoverageEnum.VERIFIED.name &&
            existing.propsHash == propsHash
        ) {
            val sameSource = existing.basedOnVersion == basedOnVersion && existing.sourceCommit == sourceCommit
            return if (sameSource) WriteAction.SKIP_VERIFIED else WriteAction.MARK_PENDING_COMPARE
        }
        return WriteAction.OVERWRITE
    }

    /**
     * 覆盖后发布链路已看到更新的版本时保留过期标记
     */
    fun keepStaleAfterOverwrite(lastSeenVersion: String?, basedOnVersion: String): Boolean {
        if (lastSeenVersion.isNullOrBlank() || !isValidVersion(lastSeenVersion)) {
            return false
        }
        return StoreUtils.isGreaterVersion(lastSeenVersion, basedOnVersion)
    }

    fun buildCardContract(contract: Map<String, Any?>): Map<String, Any?> {
        val card = linkedMapOf<String, Any?>()
        CARD_PLAIN_SECTIONS.filter { contract.containsKey(it) }.forEach { card[it] = contract[it] }
        card["risks"] = pickFields(
            items = listItems(contract["risks"]).filter { it["severity"] in CARD_RISK_SEVERITIES },
            fields = CARD_RISK_FIELDS
        )
        card["constraints"] = pickFields(listItems(contract["constraints"]), CARD_CONSTRAINT_FIELDS)
        card["outputs"] = pickFields(listItems(contract["outputs"]), CARD_OUTPUT_FIELDS)
        card["examples"] = listItems(contract["examples"]).take(1)
        return card
    }

    private fun listItems(section: Any?): List<Map<*, *>> {
        return (section as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
    }

    private fun pickFields(items: List<Map<*, *>>, fields: List<String>): List<Map<String, Any?>> {
        return items.map { item -> fields.filter { item.containsKey(it) }.associateWith { item[it] } }
    }
}
