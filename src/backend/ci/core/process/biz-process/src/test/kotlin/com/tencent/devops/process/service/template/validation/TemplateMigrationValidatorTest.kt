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

package com.tencent.devops.process.service.template.validation

import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.migration.ValidationRuleType
import com.tencent.devops.process.pojo.template.migration.ValidationSeverity
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TemplateMigrationValidatorTest : BkCiAbstractTest() {

    private val templateDao: TemplateDao = mockk(relaxed = true)
    private val pipelineTemplateInfoService: PipelineTemplateInfoService = mockk(relaxed = true)
    private val pipelineTemplateResourceService: PipelineTemplateResourceService =
        mockk(relaxed = true)

    private lateinit var integrityValidator: TemplateMigrationIntegrityValidator

    companion object {
        private const val PROJECT_ID = "test-project"
        private const val TEMPLATE_ID = "tpl-12345"
    }

    @BeforeEach
    fun setup() {
        integrityValidator = TemplateMigrationIntegrityValidator(
            dslContext = dslContext,
            templateDao = templateDao,
            pipelineTemplateInfoService = pipelineTemplateInfoService,
            pipelineTemplateResourceService = pipelineTemplateResourceService
        )
    }

    @Nested
    @DisplayName("完整性验证器测试")
    inner class IntegrityValidatorTest {

        @Test
        @DisplayName("验证器类型应为INTEGRITY")
        fun `validator type should be INTEGRITY`() {
            Assertions.assertEquals(ValidationRuleType.INTEGRITY, integrityValidator.getType())
        }

        @Test
        @DisplayName("当V1和V2模板数量一致时，验证通过")
        fun `validate passes when template counts match`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                templateDao.countTemplateVersions(dslContext, PROJECT_ID, TEMPLATE_ID, any())
            } returns 2
            every {
                pipelineTemplateResourceService.countVersions(PROJECT_ID, TEMPLATE_ID)
            } returns 2

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            Assertions.assertTrue(discrepancies.isEmpty())
        }

        @Test
        @DisplayName("当V1和V2模板数量不一致时，返回差异")
        fun `validate returns discrepancy when template counts mismatch`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns listOf(TEMPLATE_ID, "tpl-67890")
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                templateDao.countTemplateVersions(dslContext, PROJECT_ID, any(), any())
            } returns 1
            every {
                pipelineTemplateResourceService.countVersions(PROJECT_ID, any())
            } returns 1

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            Assertions.assertTrue(discrepancies.isNotEmpty())
            val countDiscrepancy = discrepancies.find { it.ruleId == "INT-001" }
            Assertions.assertNotNull(countDiscrepancy)
            Assertions.assertEquals(
                ValidationSeverity.CRITICAL,
                countDiscrepancy?.severity
            )
            Assertions.assertEquals("2", countDiscrepancy?.v1Value)
            Assertions.assertEquals("1", countDiscrepancy?.v2Value)
        }

        @Test
        @DisplayName("当V1没有模板时，V2也应该没有模板")
        fun `validate passes when both V1 and V2 have no templates`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns emptyList()
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns emptyList()

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            Assertions.assertTrue(discrepancies.isEmpty())
        }

        @Test
        @DisplayName("检测版本数量不一致")
        fun `validate detects version count mismatch`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                templateDao.countTemplateVersions(dslContext, PROJECT_ID, TEMPLATE_ID, any())
            } returns 5
            every {
                pipelineTemplateResourceService.countVersions(PROJECT_ID, TEMPLATE_ID)
            } returns 3

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            val versionDiscrepancy = discrepancies.find { it.ruleId == "INT-002" }
            Assertions.assertNotNull(versionDiscrepancy)
            Assertions.assertEquals(TEMPLATE_ID, versionDiscrepancy?.templateId)
            Assertions.assertEquals("5", versionDiscrepancy?.v1Value)
            Assertions.assertEquals("3", versionDiscrepancy?.v2Value)
        }

        @Test
        @DisplayName("检测V1中存在但V2中缺失的模板（迁移遗漏）")
        fun `validate detects missing data in V2`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns listOf(TEMPLATE_ID, "missing-tpl")
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                templateDao.countTemplateVersions(dslContext, PROJECT_ID, any(), any())
            } returns 1
            every {
                pipelineTemplateResourceService.countVersions(PROJECT_ID, any())
            } returns 1

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            val missingDiscrepancy = discrepancies.find { it.ruleId == "INT-003" }
            Assertions.assertNotNull(missingDiscrepancy)
            Assertions.assertEquals("missing-tpl", missingDiscrepancy?.templateId)
            Assertions.assertEquals(
                ValidationSeverity.CRITICAL,
                missingDiscrepancy?.severity
            )
        }

        @Test
        @DisplayName("检测孤立数据")
        fun `validate detects orphaned data in V2`() {
            // Arrange
            every {
                templateDao.listTemplateIds(dslContext, PROJECT_ID)
            } returns listOf(TEMPLATE_ID)
            every {
                pipelineTemplateInfoService.listAllIds(PROJECT_ID)
            } returns listOf(TEMPLATE_ID, "orphan-tpl")
            every {
                templateDao.countTemplateVersions(dslContext, PROJECT_ID, TEMPLATE_ID, any())
            } returns 1
            every {
                pipelineTemplateResourceService.countVersions(PROJECT_ID, TEMPLATE_ID)
            } returns 1

            // Act
            val discrepancies = integrityValidator.validate(PROJECT_ID)

            // Assert
            val orphanDiscrepancy = discrepancies.find { it.ruleId == "INT-004" }
            Assertions.assertNotNull(orphanDiscrepancy)
            Assertions.assertEquals("orphan-tpl", orphanDiscrepancy?.templateId)
            Assertions.assertEquals(
                ValidationSeverity.MEDIUM,
                orphanDiscrepancy?.severity
            )
        }
    }
}
