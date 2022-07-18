/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.query.matcher

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class RuleMatcherTest {

    @Test
    fun testMatch() {
        val valuesToMatch = generateValuesToMatch()

        // EQ
        var rule = Rule.QueryRule(FIELD_PROJECT_ID, PROJECT_ID, OperationType.EQ)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "repo-dev"), valuesToMatch))
        rule = Rule.QueryRule(
            "$FIELD_FILE.${FileChild::metadata.name}.scanStatus", "SUCCESS", OperationType.EQ
        )
        assertTrue(RuleMatcher.match(rule, valuesToMatch))

        // NE
        rule = Rule.QueryRule("$FIELD_FILE.${FileChild::name.name}", "f.zip", OperationType.NE)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = FILE_NAME), valuesToMatch))

        // LTE
        rule = Rule.QueryRule("$FIELD_FILE.${FileChild::size.name}", 1024, OperationType.LTE)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = 1023), valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = 1025), valuesToMatch))

        // LT
        rule = Rule.QueryRule("$FIELD_FILE.${FileChild::size.name}", 1024, OperationType.LT)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = 1025), valuesToMatch))

        // GTE
        rule = Rule.QueryRule("$FIELD_FILE.${FileChild::size.name}", 1024, OperationType.GTE)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = 1023), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = 1025), valuesToMatch))

        // GT
        rule = Rule.QueryRule("$FIELD_FILE.${FileChild::size.name}", 1024, OperationType.GT)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = 1023), valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = 1023.2), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = 1024.3f), valuesToMatch))

        // BEFORE
        val createdDateTime = valuesToMatch[FIELD_CREATED_DATETIME] as LocalDateTime
        rule = Rule.QueryRule(FIELD_CREATED_DATETIME, createdDateTime.plusDays(1), OperationType.BEFORE)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = createdDateTime.minusDays(1)), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(field = "notExists"), valuesToMatch))

        // AFTER
        rule = Rule.QueryRule(FIELD_CREATED_DATETIME, createdDateTime.plusDays(1), OperationType.AFTER)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = createdDateTime.minusDays(1)), valuesToMatch))

        // IN
        rule = Rule.QueryRule(FIELD_TAGS, listOf("txt"), OperationType.IN)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = listOf("apk", "txt")), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = listOf("apk")), valuesToMatch))
        rule = Rule.QueryRule(FIELD_PROJECT_ID, listOf(PROJECT_ID), OperationType.IN)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = listOf("repo-dev")), valuesToMatch))
        assertThrows<IllegalArgumentException> {
            RuleMatcher.match(rule.copy(field = "emptyList"), valuesToMatch)
        }
        assertThrows<IllegalArgumentException> {
            RuleMatcher.match(rule.copy(value = emptyList<String>()), valuesToMatch)
        }

        // NIN
        rule = Rule.QueryRule(FIELD_TAGS, listOf("txt"), OperationType.NIN)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = listOf("apk", "txt")), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = listOf("txt", "apk")), valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = listOf("apk")), valuesToMatch))
        rule = Rule.QueryRule(FIELD_PROJECT_ID, listOf("repo-dev"), OperationType.NIN)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = listOf(PROJECT_ID)), valuesToMatch))
        assertThrows<IllegalArgumentException> {
            RuleMatcher.match(rule.copy(field = "emptyList"), valuesToMatch)
        }
        assertThrows<IllegalArgumentException> {
            RuleMatcher.match(rule.copy(value = emptyList<String>()), valuesToMatch)
        }

        // PREFIX
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "repo-", OperationType.PREFIX)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "rep-"), valuesToMatch))

        // SUFFIX
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "-test", OperationType.SUFFIX)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "-est"), valuesToMatch))

        // MATCH
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "repo-*-test", OperationType.MATCH)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "REPO-*-test"), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "rep-*-test"), valuesToMatch))

        // MATCH_I
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "REPO-*-test", OperationType.MATCH_I)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule.copy(value = "repo-*-test"), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "rep-*-test"), valuesToMatch))

        // REGEX
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "[\\w]+-dev-.*", OperationType.REGEX)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(value = "test.*"), valuesToMatch))

        // NULL
        rule = Rule.QueryRule("$FIELD_FILE.notExistsField", "", OperationType.NULL)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(field = FIELD_PROJECT_ID), valuesToMatch))

        // NOT_NULL
        rule = Rule.QueryRule(FIELD_PROJECT_ID, "", OperationType.NOT_NULL)
        assertTrue(RuleMatcher.match(Rule.FixedRule(rule), valuesToMatch))
        assertFalse(RuleMatcher.match(rule.copy(field = "notExists"), valuesToMatch))
    }

    @Test
    fun testSuccessOnMiss() {
        val valuesToMatch = generateValuesToMatch()
        val rule = Rule.QueryRule("fieldNotExists", "other", OperationType.EQ)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        assertTrue(RuleMatcher.match(rule, valuesToMatch, true))
    }

    @Test
    fun testMatchOr() {
        val valuesToMatch = generateValuesToMatch()
        val projectIdRule = Rule.QueryRule(FIELD_PROJECT_ID, PROJECT_ID, OperationType.EQ)
        val repoNameRule = Rule.QueryRule(FIELD_REPO_NAME, "other", OperationType.EQ)
        var rule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule), Rule.NestedRule.RelationType.OR)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
        rule = Rule.NestedRule(
            mutableListOf(projectIdRule.copy(value = "other"), repoNameRule),
            Rule.NestedRule.RelationType.OR
        )
        assertFalse(RuleMatcher.match(rule, valuesToMatch))

        assertTrue(RuleMatcher.match(rule.copy(rules = mutableListOf()), valuesToMatch))
    }

    @Test
    fun testMatchAnd() {
        val valuesToMatch = generateValuesToMatch()
        val projectIdRule = Rule.QueryRule(FIELD_PROJECT_ID, PROJECT_ID, OperationType.EQ)
        val repoNameRule = Rule.QueryRule(FIELD_REPO_NAME, "other", OperationType.EQ)
        var rule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule), Rule.NestedRule.RelationType.AND)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        rule = Rule.NestedRule(
            mutableListOf(projectIdRule, repoNameRule.copy(value = REPO_NAME)),
            Rule.NestedRule.RelationType.AND
        )
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
    }

    @Test
    fun testMatchNor() {
        val valuesToMatch = generateValuesToMatch()
        val projectIdRule = Rule.QueryRule(FIELD_PROJECT_ID, PROJECT_ID, OperationType.EQ)
        val repoNameRule = Rule.QueryRule(FIELD_REPO_NAME, "other", OperationType.EQ)
        var rule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule), Rule.NestedRule.RelationType.NOR)
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
        rule = Rule.NestedRule(
            mutableListOf(projectIdRule.copy(value = "other"), repoNameRule),
            Rule.NestedRule.RelationType.NOR
        )
        assertTrue(RuleMatcher.match(rule, valuesToMatch))
    }

    @Test
    fun testOrAnd() {
        val valuesToMatch = generateValuesToMatch()
        val projectIdRule = Rule.QueryRule(FIELD_PROJECT_ID, PROJECT_ID, OperationType.EQ)
        val repoNameRule = Rule.QueryRule(FIELD_REPO_NAME, listOf(REPO_NAME, "pipeline"), OperationType.IN)

        val fileNameField = "$FIELD_FILE.${FileChild::name.name}"
        val nameRule1 = Rule.QueryRule(fileNameField, "te.txt", OperationType.EQ)
        val nameRule2 = Rule.QueryRule(fileNameField, "*test*", OperationType.MATCH)
        val nameRule3 = Rule.QueryRule(fileNameField, "*.tx", OperationType.MATCH)
        val orRule = Rule.NestedRule(mutableListOf(nameRule1, nameRule2, nameRule3), Rule.NestedRule.RelationType.OR)

        var rule = Rule.NestedRule(mutableListOf(projectIdRule, repoNameRule, orRule), Rule.NestedRule.RelationType.AND)
        assertTrue(RuleMatcher.match(rule, valuesToMatch))

        rule = Rule.NestedRule(
            mutableListOf(projectIdRule.copy(value = "other"), repoNameRule, orRule),
            Rule.NestedRule.RelationType.AND
        )
        assertFalse(RuleMatcher.match(rule, valuesToMatch))

        rule = Rule.NestedRule(
            mutableListOf(
                projectIdRule,
                repoNameRule,
                Rule.NestedRule(mutableListOf(nameRule1, nameRule3), Rule.NestedRule.RelationType.OR)
            ),
            Rule.NestedRule.RelationType.AND
        )
        assertFalse(RuleMatcher.match(rule, valuesToMatch))
    }

    private fun generateValuesToMatch(): Map<String, Any> {
        val metadata = mapOf(
            "scanStatus" to "SUCCESS"
        )
        return mapOf(
            FIELD_PROJECT_ID to PROJECT_ID,
            FIELD_REPO_NAME to REPO_NAME,
            "fullPath" to "/aa/$FILE_NAME",
            FIELD_CREATED_DATETIME to LocalDateTime.parse("2022-03-29T14:27:08.493", DateTimeFormatter.ISO_DATE_TIME),
            FIELD_FILE to FileChild(1024L, metadata),
            FIELD_TAGS to listOf("txt", "text"),
            "emptyList" to emptyList<String>()
        )
    }

    private open class FileParent(val name: String)
    private data class FileChild(val size: Long, val metadata: Map<String, String>) : FileParent(FILE_NAME)

    companion object {
        private const val PROJECT_ID = "repo-dev-test"
        private const val REPO_NAME = "custom"
        private const val FIELD_FILE = "file"
        private const val FIELD_TAGS = "tags"
        private const val FIELD_CREATED_DATETIME = "createdDateTime"
        private const val FIELD_PROJECT_ID = "projectId"
        private const val FIELD_REPO_NAME = "repoName"
        private const val FILE_NAME = "test.txt"
    }
}
