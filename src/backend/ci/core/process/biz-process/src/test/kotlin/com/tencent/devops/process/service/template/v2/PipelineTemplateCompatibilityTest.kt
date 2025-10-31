package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompatibilityCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.template.v2.version.convert.PipelineTemplateCompatibilityCreateReqConverter
import com.tencent.devops.process.service.template.v2.version.processor.PTemplateCompatibilityVersionPostProcessor
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class])
class PipelineTemplateCompatibilityTest : BkCiAbstractTest() {
    @Test
    @DisplayName("converter: v1计数+v2兜底确保版本名唯一")
    fun testConverterEnsureUniqueVersionName() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val generator: PipelineTemplateGenerator = mockk()
        val initializer: PipelineTemplateModelInitializer = mockk()
        val templateDao: TemplateDao = mockk()
        val dsl: DSLContext = dslContext
        val resourceService: PipelineTemplateResourceService = mockk()

        val model = newModel(userId)
        val setting = newSetting(projectId, templateId, userId)

        stubTransfer(generator)

        // v1已存在一次 -> 基础名为 v1-2
        every { templateDao.countTemplateVersions(dsl, projectId, templateId, v1VersionName) } returns 1
        // v2存在 v1-2 -> 兜底增量变为 v1-3
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = "v1-2",
                includeDelete = false
            )
        } returns mockk(relaxed = true)
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = match { it != "v1-2" },
                includeDelete = false
            )
        } returns null

        justRun { initializer.initTemplateModel(any()) }

        val converter = PipelineTemplateCompatibilityCreateReqConverter(
            pipelineTemplateGenerator = generator,
            pipelineTemplateModelInitializer = initializer,
            templateDao = templateDao,
            dslContext = dsl,
            pipelineTemplateResourceService = resourceService
        )

        val ctx = converter.convert(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = null,
            request = PipelineTemplateCompatibilityCreateReq(
                model = model,
                setting = setting,
                v1VersionName = v1VersionName,
                category = null,
                logoUrl = null
            )
        )
        Assertions.assertEquals("v1-3", ctx.customVersionName)
    }

    @Test
    @DisplayName("converter: 正常无重名，直接保留v1VersionName")
    fun testConverterNoDuplicateKeepName() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val generator: PipelineTemplateGenerator = mockk()
        val initializer: PipelineTemplateModelInitializer = mockk()
        val templateDao: TemplateDao = mockk()
        val dsl: DSLContext = dslContext
        val resourceService: PipelineTemplateResourceService = mockk()

        val model = newModel(userId)
        val setting = newSetting(projectId, templateId, userId)

        stubTransfer(generator)
        every { templateDao.countTemplateVersions(dsl, projectId, templateId, v1VersionName) } returns 0
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = v1VersionName,
                includeDelete = false
            )
        } returns null
        justRun { initializer.initTemplateModel(any()) }

        val converter = PipelineTemplateCompatibilityCreateReqConverter(
            pipelineTemplateGenerator = generator,
            pipelineTemplateModelInitializer = initializer,
            templateDao = templateDao,
            dslContext = dsl,
            pipelineTemplateResourceService = resourceService
        )

        val ctx = converter.convert(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = null,
            request = PipelineTemplateCompatibilityCreateReq(
                model = model,
                setting = setting,
                v1VersionName = v1VersionName,
                category = null,
                logoUrl = null
            )
        )
        Assertions.assertEquals("v1", ctx.customVersionName)
    }

    @Test
    @DisplayName("converter: v1重名但v2不重名，采用 v1-2")
    fun testConverterV1DuplicateButV2NotDuplicate() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val generator: PipelineTemplateGenerator = mockk()
        val initializer: PipelineTemplateModelInitializer = mockk()
        val templateDao: TemplateDao = mockk()
        val dsl: DSLContext = dslContext
        val resourceService: PipelineTemplateResourceService = mockk()

        val model = newModel(userId)
        val setting = newSetting(projectId, templateId, userId)

        stubTransfer(generator)
        // v1已有一次 -> 候选名 v1-2
        every { templateDao.countTemplateVersions(dsl, projectId, templateId, v1VersionName) } returns 1
        // v2 不存在 v1-2 -> 直接使用 v1-2
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = "v1-2",
                includeDelete = false
            )
        } returns null
        justRun { initializer.initTemplateModel(any()) }

        val converter = PipelineTemplateCompatibilityCreateReqConverter(
            pipelineTemplateGenerator = generator,
            pipelineTemplateModelInitializer = initializer,
            templateDao = templateDao,
            dslContext = dsl,
            pipelineTemplateResourceService = resourceService
        )

        val ctx = converter.convert(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = null,
            request = PipelineTemplateCompatibilityCreateReq(
                model = model,
                setting = setting,
                v1VersionName = v1VersionName,
                category = null,
                logoUrl = null
            )
        )
        Assertions.assertEquals("v1-2", ctx.customVersionName)

        // 同一场景追加：v2 已存在 v1（plain）需改名为 v1-1，并完成 v1 双写
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val txDsl: DSLContext = DSL.using(dslContext.configuration())

        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = v1VersionName,
                includeDelete = false
            )
        } returns mockk(relaxed = true)
        every { resourceService.update(any(), any(), any()) } returns 1
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(any(), any(), any()) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = txDsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId)
        val without = PTemplateResourceWithoutVersion(
            projectId = projectId,
            templateId = templateId,
            type = PipelineTemplateType.PIPELINE,
            model = model,
            yaml = null,
            status = VersionStatus.RELEASED,
            creator = userId
        )
        val only = PTemplateResourceOnlyVersion(
            version = 300L,
            number = 2,
            versionName = ctx.customVersionName, // v1-2
            versionNum = 2,
            pipelineVersion = 2,
            triggerVersion = 2,
            settingVersion = 2,
            settingVersionNum = 2
        )
        val res = PipelineTemplateResource(
            pTemplateResourceWithoutVersion = without,
            pTemplateResourceOnlyVersion = only
        )
        val ctxForPost = PipelineTemplateVersionCreateContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            v1VersionName = v1VersionName,
            customVersionName = ctx.customVersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(txDsl, ctxForPost, res, setting)

        verify(exactly = 1) { resourceService.update(any(), any(), any()) }
        verify(exactly = 1) { v1SettingDao.saveSetting(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: 命中同名时重命名(-1)并继续回写v1")
    fun testPostProcessorRenameAndDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        // 命中同名，触发改名
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = v1VersionName,
                includeDelete = false
            )
        } returns mockk(relaxed = true)
        every { resourceService.update(any(), any(), any()) } returns 1
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(any(), any(), any()) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId)
        val model = Model.defaultModel("name", userId)
        val without = PTemplateResourceWithoutVersion(
            projectId = projectId,
            templateId = templateId,
            type = PipelineTemplateType.PIPELINE,
            model = model,
            yaml = null,
            status = VersionStatus.RELEASED,
            creator = userId
        )
        val only = PTemplateResourceOnlyVersion(
            version = 200L,
            number = 1,
            versionName = "v1",
            versionNum = 1,
            pipelineVersion = 1,
            triggerVersion = 1,
            settingVersion = 1,
            settingVersionNum = 1
        )
        val res = PipelineTemplateResource(
            pTemplateResourceWithoutVersion = without,
            pTemplateResourceOnlyVersion = only
        )
        val setting = PipelineSetting(
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = "name",
            desc = "",
            pipelineAsCodeSettings = null,
            creator = userId,
            updater = userId
        )
        val ctx = PipelineTemplateVersionCreateContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            v1VersionName = v1VersionName,
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)

        // v2 改名 + v1 回写均发生
        verify(exactly = 1) { resourceService.update(any(), any(), any()) }
        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = any(),
                projectId = projectId,
                templateId = templateId,
                templateName = any(),
                versionName = any(),
                userId = any(),
                template = any(),
                type = any(),
                category = any(),
                logoUrl = any(),
                srcTemplateId = any(),
                storeFlag = any(),
                weight = any(),
                version = any(),
                desc = any()
            )
        }
        verify(exactly = 1) { v1SettingDao.saveSetting(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: 改名冲突抛错时仅告警不影响v1回写")
    fun testPostProcessorRenameConflictSkip() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = v1VersionName,
                includeDelete = false
            )
        } returns mockk(relaxed = true)
        every { resourceService.update(any(), any(), any()) } throws RuntimeException("dup key")
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(any(), any(), any()) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId)
        val model = Model.defaultModel("name", userId)
        val without = PTemplateResourceWithoutVersion(
            projectId = projectId,
            templateId = templateId,
            type = PipelineTemplateType.PIPELINE,
            model = model,
            yaml = null,
            status = VersionStatus.RELEASED,
            creator = userId
        )
        val only = PTemplateResourceOnlyVersion(
            version = 200L,
            number = 1,
            versionName = "v1",
            versionNum = 1,
            pipelineVersion = 1,
            triggerVersion = 1,
            settingVersion = 1,
            settingVersionNum = 1
        )
        val res = PipelineTemplateResource(
            pTemplateResourceWithoutVersion = without,
            pTemplateResourceOnlyVersion = only
        )
        val setting = PipelineSetting(
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = "name",
            desc = "",
            pipelineAsCodeSettings = null,
            creator = userId,
            updater = userId
        )
        val ctx = PipelineTemplateVersionCreateContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            v1VersionName = v1VersionName,
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        Assertions.assertDoesNotThrow {
            post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)
        }
        // 改名失败被捕获但不影响 v1 回写
        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = any(),
                projectId = projectId,
                templateId = templateId,
                templateName = any(),
                versionName = any(),
                userId = any(),
                template = any(),
                type = any(),
                category = any(),
                logoUrl = any(),
                srcTemplateId = any(),
                storeFlag = any(),
                weight = any(),
                version = any(),
                desc = any()
            )
        }
        verify(exactly = 1) { v1SettingDao.saveSetting(any(), any(), any()) }
    }

    private fun buildTemplateInfo(
        projectId: String,
        templateId: String,
        name: String,
        creator: String
    ) = PipelineTemplateInfoV2(
        id = templateId,
        projectId = projectId,
        name = name,
        desc = null,
        mode = TemplateType.CUSTOMIZE,
        type = PipelineTemplateType.PIPELINE,
        enablePac = false,
        latestVersionStatus = VersionStatus.RELEASED,
        creator = creator
    )

}

private fun stubTransfer(generator: PipelineTemplateGenerator) {
    every {
        generator.transfer(
            userId = any(),
            projectId = any(),
            storageType = PipelineStorageType.MODEL,
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = any(),
            templateSetting = any(),
            params = any(),
            yaml = null
        )
    } answers {
        val tm = arg<ITemplateModel?>(4)
        val st = arg<PipelineSetting?>(5)
        val params = (tm as Model).getTriggerContainer().params
        PTemplateModelTransferResult(
            templateType = PipelineTemplateType.PIPELINE,
            templateModel = tm,
            templateSetting = st!!,
            params = params,
            yamlWithVersion = null
        )
    }
}

private fun newModel(userId: String): Model = Model.defaultModel("name", userId)

private fun newSetting(
    projectId: String,
    templateId: String,
    userId: String
): PipelineSetting = PipelineSetting(
    projectId = projectId,
    pipelineId = templateId,
    pipelineName = "name",
    desc = "",
    pipelineAsCodeSettings = null,
    creator = userId,
    updater = userId
)

private fun stubV1CreateTemplate(v1Dao: TemplateDao) {
    every {
        v1Dao.createTemplate(
            dslContext = any(),
            projectId = any(),
            templateId = any(),
            templateName = any(),
            versionName = any(),
            userId = any(),
            template = any(),
            type = any(),
            category = any(),
            logoUrl = any(),
            srcTemplateId = any(),
            storeFlag = any(),
            weight = any(),
            version = any(),
            desc = any()
        )
    } returns 100
}
