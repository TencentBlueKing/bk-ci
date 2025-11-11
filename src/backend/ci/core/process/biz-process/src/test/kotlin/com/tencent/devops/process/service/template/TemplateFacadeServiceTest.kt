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
import org.jooq.Record5
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
            pipelineEventDispatcher = mockk(relaxed = true)
        )
    }

    @AfterEach
    fun tearDown() {
        // 清理 mock 对象
        unmockkObject(MessageUtil)
        unmockkObject(I18nUtil)
    }

    @Test
    @DisplayName("降序：3个相同版本名，最新版本保留原名，旧版本添加后缀")
    fun testDescendingDuplicateVersionNames() {
        val projectId = "project1"
        val templateId = "template1"
        val versionName = "init"

        // Mock 数据：3个版本，名称都是 "init"
        // 使用真实的日期时间：2024-03-15 > 2024-03-10 > 2024-03-05
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    1L, versionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),  // 最新
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    2L, versionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),  // 次新
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    3L, versionName,
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),   // 最旧
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
        Assertions.assertEquals("init", result[0].versionName)     // 最新版本保留原名
        Assertions.assertEquals("init-1", result[1].versionName)   // 次新版本加后缀
        Assertions.assertEquals("init-2", result[2].versionName)   // 最旧版本加后缀
    }

    @Test
    @DisplayName("升序：3个相同版本名，最新版本保留原名，旧版本添加后缀")
    fun testAscendingDuplicateVersionNames() {
        val projectId = "project1"
        val templateId = "template1"
        val versionName = "init"

        // Mock 数据：3个版本，名称都是 "init"，按 updateTime 升序排列
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    3L, versionName,
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),   // 最旧
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    2L, versionName,
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),  // 次新
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    1L, versionName,
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),  // 最新
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
        Assertions.assertEquals("init-1", result[0].versionName)   // 最旧版本加后缀
        Assertions.assertEquals("init-2", result[1].versionName)   // 次新版本加后缀
        Assertions.assertEquals("init", result[2].versionName)     // 最新版本保留原名
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
                    1L, "v2.0",
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    "user1"
                ),   // v2.0 唯一
                VersionData(
                    2L, "init",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),   // init 最新
                VersionData(
                    3L, "init",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),   // init 次新
                VersionData(
                    4L, "init",
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                ),   // init 最旧
                VersionData(
                    5L, "v1.0",
                    LocalDateTime.of(2024, 3, 1, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 1, 10, 0, 0),
                    "user1"
                )    // v1.0 唯一
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(5, result.size)
        Assertions.assertEquals("v2.0", result[0].versionName)     // 唯一版本
        Assertions.assertEquals("init", result[1].versionName)     // init 最新版本
        Assertions.assertEquals("init-1", result[2].versionName)   // init 次新版本
        Assertions.assertEquals("init-2", result[3].versionName)   // init 最旧版本
        Assertions.assertEquals("v1.0", result[4].versionName)     // 唯一版本
    }

    @Test
    @DisplayName("所有版本名都唯一，保持原样")
    fun testAllUniqueVersionNames() {
        val projectId = "project1"
        val templateId = "template1"

        // Mock 数据：所有版本名都不重复
        val mockRecords = createMockRecords(
            listOf(
                VersionData(
                    1L, "v3.0",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    2L, "v2.0",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),
                VersionData(
                    3L, "v1.0",
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
        Assertions.assertEquals("v2.0", result[1].versionName)
        Assertions.assertEquals("v1.0", result[2].versionName)
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
                VersionData(3L, versionName, sameUpdateTime, sameUpdateTime, "user1"), // VERSION 最大，最新
                VersionData(2L, versionName, sameUpdateTime, sameUpdateTime, "user1"),
                VersionData(1L, versionName, sameUpdateTime, sameUpdateTime, "user1")  // VERSION 最小，最旧
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(3, result.size)
        // 第一个出现的（VERSION=3）是最新版本，保留原名
        Assertions.assertEquals("init", result[0].versionName)
        Assertions.assertEquals(3L, result[0].version)
        // 其他添加后缀
        Assertions.assertEquals("init-1", result[1].versionName)
        Assertions.assertEquals(2L, result[1].version)
        Assertions.assertEquals("init-2", result[2].versionName)
        Assertions.assertEquals(1L, result[2].version)
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
                    1L, "v2.0",
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 20, 10, 0, 0),
                    "user1"
                ),  // v2.0 最新
                VersionData(
                    2L, "v2.0",
                    LocalDateTime.of(2024, 3, 18, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 18, 10, 0, 0),
                    "user1"
                ),  // v2.0 旧
                VersionData(
                    3L, "init",
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 15, 10, 0, 0),
                    "user1"
                ),  // init 最新
                VersionData(
                    4L, "init",
                    LocalDateTime.of(2024, 3, 12, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 12, 10, 0, 0),
                    "user1"
                ),  // init 旧
                VersionData(
                    5L, "v1.0",
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 10, 10, 0, 0),
                    "user1"
                ),  // v1.0 最新
                VersionData(
                    6L, "v1.0",
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    LocalDateTime.of(2024, 3, 5, 10, 0, 0),
                    "user1"
                )   // v1.0 旧
            )
        )

        every {
            templateDao.getTemplateVersionInfos(dslContext, projectId, templateId, false)
        } returns mockRecords

        val result = service.listTemplateAllVersions(projectId, templateId, false)

        Assertions.assertEquals(6, result.size)
        Assertions.assertEquals("v2.0", result[0].versionName)     // v2.0 最新版本
        Assertions.assertEquals("v2.0-1", result[1].versionName)   // v2.0 旧版本
        Assertions.assertEquals("init", result[2].versionName)     // init 最新版本
        Assertions.assertEquals("init-1", result[3].versionName)   // init 旧版本
        Assertions.assertEquals("v1.0", result[4].versionName)     // v1.0 最新版本
        Assertions.assertEquals("v1.0-1", result[5].versionName)   // v1.0 旧版本
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
