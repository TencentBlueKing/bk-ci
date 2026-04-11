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
    @DisplayName("converter: v1VersionName映射为customVersionName")
    fun testConverterMapsV1VersionNameToCustomVersionName() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val generator: PipelineTemplateGenerator = mockk()
        val initializer: PipelineTemplateModelInitializer = mockk()
        val pipelineTemplateInfoService: PipelineTemplateInfoService = mockk()

        val model = newModel(userId)
        val setting = newSetting(projectId, templateId, userId)

        stubTransfer(generator)

        every {
            pipelineTemplateInfoService.getOrNull(
                projectId = projectId,
                templateId = templateId
            )
        } returns null

        justRun { initializer.initTemplateModel(any()) }

        val converter = PipelineTemplateCompatibilityCreateReqConverter(
            pipelineTemplateGenerator = generator,
            pipelineTemplateModelInitializer = initializer,
            pipelineTemplateInfoService = pipelineTemplateInfoService
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
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 使用customVersionName做v1双写")
    fun testPostProcessorInTransactionUsesCustomVersionName() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val customVersionName = "v1"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 0
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(any(), any(), any()) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
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
            customVersionName = customVersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)

        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = any(),
                projectId = projectId,
                templateId = templateId,
                templateName = any(),
                versionName = customVersionName,
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
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 非CREATE_RELEASE时直接返回")
    fun testPostProcessorInTransactionSkipWhenNotReleaseVersion() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        val post = PTemplateCompatibilityVersionPostProcessor(
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
            customVersionName = "v1",
            versionAction = PipelineVersionAction.SAVE_DRAFT,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)

        verify(exactly = 0) {
            v1Dao.createTemplate(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()
            )
        }
        verify(exactly = 0) { v1SettingDao.saveSetting(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate strictMode下双写失败抛异常")
    fun testPostProcessorInTransactionDualWriteFailureThrows() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 0
        every {
            v1Dao.createTemplate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()
            )
        } throws RuntimeException("v1 insert failed")

        val post = PTemplateCompatibilityVersionPostProcessor(
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
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        Assertions.assertThrows(RuntimeException::class.java) {
            post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)
        }
    }

    @Test
    @DisplayName("postProcessor: 约束模板存量升级时跳过双写")
    fun testPostProcessorInTransactionSkipConstraintTemplateDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId).copy(
            mode = TemplateType.CONSTRAINT
        )
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
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)

        verify(exactly = 1) { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) }
        verify(exactly = 0) {
            v1Dao.createTemplate(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()
            )
        }
        verify(exactly = 0) { v1SettingDao.saveSetting(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: 约束模板首次创建时正常双写")
    fun testPostProcessorInTransactionConstraintTemplateFirstCreateDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val customVersionName = "v1"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 0
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(dsl, any(), true) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId).copy(
            mode = TemplateType.CONSTRAINT
        )
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
            customVersionName = customVersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)

        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = dsl,
                projectId = projectId,
                templateId = templateId,
                templateName = "name",
                versionName = customVersionName,
                userId = userId,
                template = any(),
                type = TemplateType.CONSTRAINT.name,
                category = any(),
                logoUrl = any(),
                srcTemplateId = any(),
                storeFlag = any(),
                weight = 0,
                version = 200L,
                desc = any()
            )
        }
        verify(exactly = 1) { v1SettingDao.saveSetting(dsl, setting, true) }
    }

    @Test
    @DisplayName("postProcessor: 自定义模板始终正常双写")
    fun testPostProcessorInTransactionCustomizeTemplateAlwaysDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val customVersionName = "v1"

        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 1
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(dsl, any(), true) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        val info = buildTemplateInfo(projectId, templateId, "name", userId).copy(
            mode = TemplateType.CUSTOMIZE
        )
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
            customVersionName = customVersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionAfterVersionCreate(dsl, ctx, res, setting)

        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = dsl,
                projectId = projectId,
                templateId = templateId,
                templateName = "name",
                versionName = customVersionName,
                userId = userId,
                template = any(),
                type = TemplateType.CUSTOMIZE.name,
                category = any(),
                logoUrl = any(),
                srcTemplateId = any(),
                storeFlag = any(),
                weight = 0,
                version = 200L,
                desc = any()
            )
        }
        verify(exactly = 1) { v1SettingDao.saveSetting(dsl, setting, true) }
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
            yaml = null,
            fallbackOnError = any()
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
