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

package com.tencent.devops.process.service.template

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.jooq.Record6
import org.jooq.Result
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TemplateFacadeServiceTest : BkCiAbstractTest() {

    private lateinit var templateDao: TemplateDao
    private lateinit var service: TemplateFacadeService

    @BeforeEach
    fun setUp() {
        templateDao = mockk()
        // Mock I18nUtil 获取默认语言
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        // Mock MessageUtil 的方法
        mockkObject(MessageUtil)
        every {
            MessageUtil.getMessageByLocale(
                messageCode = ProcessMessageCode.BK_TEMPLATE_VERSION_REFACTOR_SUFFIX_DESC,
                language = any()
            )
        } returns "模版版本管理重构升级，历史同名版本名称自动添加后缀"
        // 创建真实的 service 实例，只 mock 必要的依赖
        service = TemplateFacadeService(
            dslContext = dslContext,
            redisOperation = mockk(relaxed = true),
            templateDao = templateDao,
            templatePipelineDao = mockk(relaxed = true),
            pipelineSettingDao = mockk(relaxed = true),
            pipelinePermissionService = mockk(relaxed = true),
            pipelineTemplatePermissionService = mockk(relaxed = true),
            pipelineRemoteAuthService = mockk(relaxed = true),
            pipelineInfoFacadeService = mockk(relaxed = true),
            stageTagService = mockk(relaxed = true),
            client = mockk(relaxed = true),
            objectMapper = mockk(relaxed = true),
            pipelineResourceDao = mockk(relaxed = true),
            pipelineBuildSummaryDao = mockk(relaxed = true),
            templateInstanceBaseDao = mockk(relaxed = true),
            templateInstanceItemDao = mockk(relaxed = true),
            pipelineLabelPipelineDao = mockk(relaxed = true),
            pipelineGroupService = mockk(relaxed = true),
            modelTaskIdGenerator = mockk(relaxed = true),
            modelContainerIdGenerator = mockk(relaxed = true),
            paramService = mockk(relaxed = true),
            modelCheckPlugin = mockk(relaxed = true),
            pipelineSettingFacadeService = mockk(relaxed = true),
            templateCommonService = mockk(relaxed = true),
            templateSettingService = mockk(relaxed = true),
            pipelineAsCodeService = mockk(relaxed = true),
            pipelineTemplateVersionManager = mockk(relaxed = true),
            pipelineTemplateMarketFacadeService = mockk(relaxed = true),
            pipelineTemplateResourceService = mockk(relaxed = true),
            pipelineEventDispatcher = mockk(relaxed = true),
            publicVarGroupReferManageService = mockk(relaxed = true),
        )
    }

    @AfterEach
    fun tearDown() {
        // 清理 mock 对象
        unmockkObject(MessageUtil)
        unmockkObject(I18nUtil)
    }

    @Test
    @DisplayName("降序：3个相同版本名，最新版本保留原名，旧版本添加version后缀")
    fun testDescendingDuplicateVersionNames() {
        val projectId = "project1"
        val templateId = "template1"
        val versionName = "init"

        // Mock 数据：3个版本，名称都是 "init"
        // 使用真实的日期时间：2024-03-15 > 2024-03-10 > 2024-03-05
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    300L, versionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0), // 最新
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    200L, versionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0), // 次新
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    100L, versionName,
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0), // 最旧
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                )
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(3, result.size)
        // 最新版本保留原名，不标记为重复
        Assertions.assertEquals("init", result[0].versionName)
        Assertions.assertEquals(300L, result[0].version)
        Assertions.assertEquals(false, result[0].nameDuplicated)
        // 次新版本添加version后缀，标记为重复
        Assertions.assertEquals("init-200", result[1].versionName)
        Assertions.assertEquals(200L, result[1].version)
        Assertions.assertEquals(true, result[1].nameDuplicated)
        // 最旧版本添加version后缀，标记为重复
        Assertions.assertEquals("init-100", result[2].versionName)
        Assertions.assertEquals(100L, result[2].version)
        Assertions.assertEquals(true, result[2].nameDuplicated)
    }

    @Test
    @DisplayName("升序：3个相同版本名，最新版本保留原名，旧版本添加version后缀")
    fun testAscendingDuplicateVersionNames() {
        val projectId = "project1"
        val templateId = "template1"
        val versionName = "init"

        // Mock 数据：3个版本，名称都是 "init"，按 updateTime 升序排列
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    100L, versionName,
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0), // 最旧
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    200L, versionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0), // 次新
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    300L, versionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0), // 最新
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                )
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, true)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, true)

        Assertions.assertEquals(3, result.size)
        // 最旧版本添加version后缀，标记为重复
        Assertions.assertEquals("init-100", result[0].versionName)
        Assertions.assertEquals(100L, result[0].version)
        Assertions.assertEquals(true, result[0].nameDuplicated)
        // 次新版本添加version后缀，标记为重复
        Assertions.assertEquals("init-200", result[1].versionName)
        Assertions.assertEquals(200L, result[1].version)
        Assertions.assertEquals(true, result[1].nameDuplicated)
        // 最新版本保留原名，不标记为重复
        Assertions.assertEquals("init", result[2].versionName)
        Assertions.assertEquals(300L, result[2].version)
        Assertions.assertEquals(false, result[2].nameDuplicated)
    }

    @Test
    @DisplayName("混合版本名：唯一版本名保持不变，重复版本名最新版保留原名")
    fun testMixedVersionNames() {
        val projectId = "project1"
        val templateId = "template1"

        // Mock 数据：v2.0唯一，init有3个，v1.0唯一，降序排列
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    500L, "v2.0",
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    "user1"
                ), // v2.0 唯一
                VersionData(
                    400L, "init",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ), // init 最新
                VersionData(
                    300L, "init",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ), // init 次新
                VersionData(
                    200L, "init",
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                ), // init 最旧
                VersionData(
                    100L, "v1.0",
                    LocalDateTime.of(2024, 3, 1, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 1, 10, 0, 0),
                    "user1"
                ) // v1.0 唯一
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(5, result.size)
        // v2.0 唯一版本，不重复
        Assertions.assertEquals("v2.0", result[0].versionName)
        Assertions.assertEquals(false, result[0].nameDuplicated)
        // init 最新版本，保留原名，不标记为重复
        Assertions.assertEquals("init", result[1].versionName)
        Assertions.assertEquals(false, result[1].nameDuplicated)
        // init 次新版本，添加version后缀，标记为重复
        Assertions.assertEquals("init-300", result[2].versionName)
        Assertions.assertEquals(true, result[2].nameDuplicated)
        // init 最旧版本，添加version后缀，标记为重复
        Assertions.assertEquals("init-200", result[3].versionName)
        Assertions.assertEquals(true, result[3].nameDuplicated)
        // v1.0 唯一版本，不重复
        Assertions.assertEquals("v1.0", result[4].versionName)
        Assertions.assertEquals(false, result[4].nameDuplicated)
    }

    @Test
    @DisplayName("所有版本名都唯一，保持原样且nameDuplicated为false")
    fun testAllUniqueVersionNames() {
        val projectId = "project1"
        val templateId = "template1"

        // Mock 数据：所有版本名都不重复
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    300L, "v3.0",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    200L, "v2.0",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    100L, "v1.0",
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                )
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(3, result.size)
        Assertions.assertEquals("v3.0", result[0].versionName)
        Assertions.assertEquals(false, result[0].nameDuplicated)
        Assertions.assertEquals("v2.0", result[1].versionName)
        Assertions.assertEquals(false, result[1].nameDuplicated)
        Assertions.assertEquals("v1.0", result[2].versionName)
        Assertions.assertEquals(false, result[2].nameDuplicated)
    }

    @Test
    @DisplayName("空列表返回空结果")
    fun testEmptyList() {
        val projectId = "project1"
        val templateId = "template1"

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns null

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(0, result.size)
    }

    @Test
    @DisplayName("相同 updateTime 的版本，按 VERSION 字段区分最新版本")
    fun testSameUpdateTimeDifferentVersion() {
        val projectId = "project1"
        val templateId = "template1"
        val versionName = "init"
        val sameUpdateTime = LocalDateTime.of(2024, 3, 15, 10, 0, 0)

        // Mock 数据：3个版本，名称都是 "init"，updateTime 相同，但 VERSION 不同
        // 降序排列时，VERSION 大的排在前面（假设数据库已按 VERSION desc 排序）
        val mockRecords = createMockRecords(
            listOf(
                VersionData(300L, versionName, sameUpdateTime, sameUpdateTime, "user1"), // VERSION 最大，最新
                VersionData(200L, versionName, sameUpdateTime, sameUpdateTime, "user1"),
                VersionData(100L, versionName, sameUpdateTime, sameUpdateTime, "user1") // VERSION 最小，最旧
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(3, result.size)
        // 第一个出现的（VERSION=300）是最新版本，保留原名
        Assertions.assertEquals("init", result[0].versionName)
        Assertions.assertEquals(300L, result[0].version)
        Assertions.assertEquals(false, result[0].nameDuplicated)
        // 其他添加version后缀
        Assertions.assertEquals("init-200", result[1].versionName)
        Assertions.assertEquals(200L, result[1].version)
        Assertions.assertEquals(true, result[1].nameDuplicated)
        Assertions.assertEquals("init-100", result[2].versionName)
        Assertions.assertEquals(100L, result[2].version)
        Assertions.assertEquals(true, result[2].nameDuplicated)
    }

    @Test
    @DisplayName("版本名过长：添加后缀后超过64位需要截断")
    fun testVersionNameTruncation() {
        val projectId = "project1"
        val templateId = "template1"
        // 创建一个60字符的版本名
        val longVersionName = "a".repeat(60)

        // Mock 数据：2个版本，名称相同且很长
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    300L, longVersionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0), // 最新
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    200L, longVersionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0), // 旧
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                )
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(2, result.size)
        // 最新版本保留原名
        Assertions.assertEquals(longVersionName, result[0].versionName)
        Assertions.assertEquals(60, result[0].versionName.length)
        Assertions.assertEquals(false, result[0].nameDuplicated)

        // 旧版本：原名(60) + 后缀(-200=4) = 64，正好等于限制，不需要截断
        Assertions.assertEquals("$longVersionName-200", result[1].versionName)
        Assertions.assertEquals(64, result[1].versionName.length)
        Assertions.assertEquals(true, result[1].nameDuplicated)
    }

    @Test
    @DisplayName("版本名过长：添加后缀超过64位需要截断原名")
    fun testVersionNameTruncationWithLongSuffix() {
        val projectId = "project1"
        val templateId = "template1"
        // 创建一个62字符的版本名
        val veryLongVersionName = "b".repeat(62)

        // Mock 数据：2个版本，名称相同且非常长
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    999999L, veryLongVersionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0), // 最新
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    888888L, veryLongVersionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0), // 旧
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                )
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(2, result.size)
        // 最新版本保留原名
        Assertions.assertEquals(veryLongVersionName, result[0].versionName)
        Assertions.assertEquals(62, result[0].versionName.length)

        // 旧版本：原名需要截断以容纳后缀 -888888 (7个字符)
        // 最终长度应该是64
        Assertions.assertTrue(result[1].versionName.length <= 64)
        Assertions.assertEquals(64, result[1].versionName.length)
        Assertions.assertTrue(result[1].versionName.endsWith("-888888"))
        // 验证截断后的格式：bbb...(57个b)-888888
        Assertions.assertEquals("${"b".repeat(57)}-888888", result[1].versionName)
    }

    @Test
    @DisplayName("多个版本名各有2个重复版本")
    fun testMultipleVersionNamesWithDuplicates() {
        val projectId = "project1"
        val templateId = "template1"

        // Mock 数据：v2.0有2个，init有2个，v1.0有2个
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    600L, "v2.0",
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    "user1"
                ), // v2.0 最新
                VersionData(
                    500L, "v2.0",
                    LocalDateTime.of(2024, 3, 18, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 18, 10, 0, 0),
                    "user1"
                ), // v2.0 旧
                VersionData(
                    400L, "init",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ), // init 最新
                VersionData(
                    300L, "init",
                    LocalDateTime.of(2024, 3, 12, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 12, 10, 0, 0),
                    "user1"
                ), // init 旧
                VersionData(
                    200L, "v1.0",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ), // v1.0 最新
                VersionData(
                    100L, "v1.0",
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                ) // v1.0 旧
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(6, result.size)
        // v2.0 最新版本，保留原名
        Assertions.assertEquals("v2.0", result[0].versionName)
        Assertions.assertEquals(false, result[0].nameDuplicated)
        // v2.0 旧版本，添加version后缀
        Assertions.assertEquals("v2.0-500", result[1].versionName)
        Assertions.assertEquals(true, result[1].nameDuplicated)
        // init 最新版本，保留原名
        Assertions.assertEquals("init", result[2].versionName)
        Assertions.assertEquals(false, result[2].nameDuplicated)
        // init 旧版本，添加version后缀
        Assertions.assertEquals("init-300", result[3].versionName)
        Assertions.assertEquals(true, result[3].nameDuplicated)
        // v1.0 最新版本，保留原名
        Assertions.assertEquals("v1.0", result[4].versionName)
        Assertions.assertEquals(false, result[4].nameDuplicated)
        // v1.0 旧版本，添加version后缀
        Assertions.assertEquals("v1.0-100", result[5].versionName)
        Assertions.assertEquals(true, result[5].nameDuplicated)
    }

    // ========== Helper Methods ==========

    /**
     * 创建 Mock 的数据库记录
     */
    private fun createMockRecords(
        versionDataList: List<VersionData>
    ): Result<Record6<Long, String, LocalDateTime, LocalDateTime, String, String>> {
        val tTemplate = TTemplate.T_TEMPLATE
        val result = DSL.using(dslContext.configuration()).newResult(
            tTemplate.VERSION,
            tTemplate.VERSION_NAME,
            tTemplate.CREATED_TIME,
            tTemplate.UPDATE_TIME,
            tTemplate.CREATOR,
            tTemplate.DESC
        )

        versionDataList.forEach { data ->
            val record = dslContext.newRecord(
                tTemplate.VERSION,
                tTemplate.VERSION_NAME,
                tTemplate.CREATED_TIME,
                tTemplate.UPDATE_TIME,
                tTemplate.CREATOR,
                tTemplate.DESC
            )
            record.setValue(tTemplate.VERSION, data.version)
            record.setValue(tTemplate.VERSION_NAME, data.versionName)
            record.setValue(tTemplate.CREATED_TIME, data.createTime)
            record.setValue(tTemplate.UPDATE_TIME, data.updateTime)
            record.setValue(tTemplate.CREATOR, data.creator)
            record.setValue(tTemplate.DESC, "")
            result.add(record)
        }

        return result
    }

    /**
     * 版本数据类，用于构造测试数据
     */
    data class VersionData(
        val version: Long,
        val versionName: String,
        val createTime: LocalDateTime,
        val updateTime: LocalDateTime,
        val creator: String
    )
}
