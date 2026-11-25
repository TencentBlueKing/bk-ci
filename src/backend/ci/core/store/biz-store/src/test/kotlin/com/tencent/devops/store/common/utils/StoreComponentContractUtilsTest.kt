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

import com.tencent.devops.store.common.utils.StoreComponentContractUtils.ContractState
import com.tencent.devops.store.common.utils.StoreComponentContractUtils.WriteAction
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractCoverageEnum
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StoreComponentContractUtilsTest {

    private fun state(
        version: String = "1.0.127",
        commit: String? = "c1",
        propsHash: String = "h1",
        coverage: StoreComponentContractCoverageEnum = StoreComponentContractCoverageEnum.AUTO_EXTRACTED,
        lastSeenVersion: String? = "1.0.127"
    ) = ContractState(
        basedOnVersion = version,
        sourceCommit = commit,
        propsHash = propsHash,
        coverage = coverage.name,
        lastSeenVersion = lastSeenVersion
    )

    @Test
    fun `isValidVersion only accepts three part versions`() {
        Assertions.assertTrue(StoreComponentContractUtils.isValidVersion("1.0.127"))
        Assertions.assertFalse(StoreComponentContractUtils.isValidVersion("1.0"))
        Assertions.assertFalse(StoreComponentContractUtils.isValidVersion("1.0.x"))
    }

    @Test
    fun `decideWrite creates when no contract exists`() {
        Assertions.assertEquals(
            WriteAction.CREATE,
            StoreComponentContractUtils.decideWrite(null, "1.0.127", "c1", "h1")
        )
    }

    @Test
    fun `decideWrite skips request older than stored contract`() {
        Assertions.assertEquals(
            WriteAction.SKIP_OUTDATED,
            StoreComponentContractUtils.decideWrite(state(version = "1.0.128"), "1.0.127", "c0", "h1")
        )
    }

    @Test
    fun `decideWrite keeps verified contract when props hash unchanged`() {
        val verified = state(coverage = StoreComponentContractCoverageEnum.VERIFIED)
        Assertions.assertEquals(
            WriteAction.MARK_PENDING_COMPARE,
            StoreComponentContractUtils.decideWrite(verified, "1.0.128", "c2", "h1")
        )
        Assertions.assertEquals(
            WriteAction.SKIP_VERIFIED,
            StoreComponentContractUtils.decideWrite(verified, "1.0.127", "c1", "h1")
        )
    }

    @Test
    fun `decideWrite overwrites verified contract when props hash changed`() {
        val verified = state(coverage = StoreComponentContractCoverageEnum.VERIFIED)
        Assertions.assertEquals(
            WriteAction.OVERWRITE,
            StoreComponentContractUtils.decideWrite(verified, "1.0.128", "c2", "h2")
        )
    }

    @Test
    fun `decideWrite overwrites auto extracted contract`() {
        Assertions.assertEquals(
            WriteAction.OVERWRITE,
            StoreComponentContractUtils.decideWrite(state(), "1.0.127", "c1", "h1")
        )
    }

    @Test
    fun `decideWrite tolerates non semver stored version`() {
        Assertions.assertEquals(
            WriteAction.OVERWRITE,
            StoreComponentContractUtils.decideWrite(state(version = "1.0"), "1.0.127", "c1", "h1")
        )
    }

    @Test
    fun `keepStaleAfterOverwrite when newer version released during extraction`() {
        Assertions.assertTrue(StoreComponentContractUtils.keepStaleAfterOverwrite("1.0.129", "1.0.128"))
        Assertions.assertFalse(StoreComponentContractUtils.keepStaleAfterOverwrite("1.0.128", "1.0.128"))
        Assertions.assertFalse(StoreComponentContractUtils.keepStaleAfterOverwrite(null, "1.0.128"))
    }

    @Test
    fun `buildCardContract keeps only high and medium risks without evidence`() {
        val contract = mapOf(
            "atom_code" to "demo",
            "summary" to "s",
            "side_effects" to listOf("x"),
            "risks" to listOf(
                mapOf(
                    "id" to "r1", "severity" to "high", "title" to "t1", "trigger" to listOf("a"),
                    "effects" to listOf("e"), "guidance" to "g1", "evidence" to listOf("A.kt:1"), "review" to "auto"
                ),
                mapOf("id" to "r2", "severity" to "low", "title" to "t2", "guidance" to "g2"),
                mapOf("id" to "r3", "severity" to "medium", "title" to "t3", "guidance" to "g3")
            ),
            "constraints" to listOf(
                mapOf(
                    "id" to "c1", "severity" to "error", "rule" to "r", "check" to mapOf("kind" to "regex"),
                    "error_code" to 1, "evidence" to listOf("A.kt:2")
                )
            ),
            "outputs" to listOf(
                mapOf("name" to "OUT", "type" to "string", "set_when" to "w", "value" to "v", "evidence" to listOf("x"))
            ),
            "examples" to listOf(mapOf("id" to "e1"), mapOf("id" to "e2"))
        )

        val card = StoreComponentContractUtils.buildCardContract(contract)

        val risks = card["risks"] as List<*>
        Assertions.assertEquals(listOf("r1", "r3"), risks.map { (it as Map<*, *>)["id"] })
        Assertions.assertEquals(setOf("id", "severity", "title", "trigger", "guidance"), (risks[0] as Map<*, *>).keys)
        Assertions.assertFalse((card["constraints"] as List<*>).any { (it as Map<*, *>).containsKey("evidence") })
        Assertions.assertEquals(
            setOf("name", "type", "set_when"),
            ((card["outputs"] as List<*>)[0] as Map<*, *>).keys
        )
        Assertions.assertEquals(1, (card["examples"] as List<*>).size)
        Assertions.assertFalse(card.containsKey("side_effects"))
        Assertions.assertEquals("demo", card["atom_code"])
    }

    @Test
    fun `buildCardContract tolerates missing sections`() {
        val card = StoreComponentContractUtils.buildCardContract(mapOf("atom_code" to "demo"))
        Assertions.assertEquals(emptyList<Any>(), card["risks"])
        Assertions.assertEquals(emptyList<Any>(), card["examples"])
    }
}
