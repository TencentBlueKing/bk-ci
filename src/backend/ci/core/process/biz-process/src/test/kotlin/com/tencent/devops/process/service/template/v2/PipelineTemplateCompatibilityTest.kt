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
    @DisplayName("converter: 直接使用v1VersionName作为期望版本名")
    fun testConverterUseV1VersionNameDirectly() {
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
        // 注意：Converter 不再处理去重逻辑，直接使用原始名称
        // 旧版本的重命名由 PTemplateCompatibilityVersionPostProcessor.postProcessBeforeVersionCreate 处理
        Assertions.assertEquals("v1", ctx.customVersionName)
    }

    @Test
    @DisplayName("postProcessor: postProcessBeforeVersionCreate 在事务外重命名旧版本")
    fun testPostProcessorBeforeCreateRenameOldVersion() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = dslContext

        // v2 已存在同名版本，需要重命名
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

        // 查找后缀时，v1-1 不存在
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = "v1-1",
                includeDelete = false
            )
        } returns null

        every { resourceService.update(any(), any(), any()) } returns 1

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
            versionName = v1VersionName,
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
            customVersionName = v1VersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        // 调用 postProcessBeforeVersionCreate，应该重命名旧版本
        post.postProcessBeforeVersionCreate(ctx, res, setting)

        // 验证：v2 旧版本被重命名（使用 version 字段作为后缀）
        // 由于我们不知道 existingVersion 的确切 version 值，只验证调用发生且包含描述
        verify(exactly = 1) {
            resourceService.update(
                transactionContext = any(),
                record = match { it.description != null },
                commonCondition = match {
                    it.projectId == projectId &&
                        it.templateId == templateId &&
                        it.versionName == v1VersionName
                }
            )
        }
    }

    @Test
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 只做v2→v1双写，不做重命名")
    fun testPostProcessorInTransactionOnlyDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

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

        // 注意：v2 重命名逻辑已移到 postProcessBeforeVersionCreate
        // 这里只验证 v1 双写
        verify(exactly = 0) { resourceService.update(any(), any(), any()) }
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
    @DisplayName("postProcessor: postProcessBeforeVersionCreate 使用version字段作为后缀")
    fun testPostProcessorBeforeCreateUseVersionAsSuffix() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "init"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = dslContext

        // v2 已存在 init 版本（version=150）
        val existingVersionMock = mockk<PipelineTemplateResource>(relaxed = true)
        every { existingVersionMock.version } returns 150L
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = "init",
                includeDelete = false
            )
        } returns existingVersionMock

        every { resourceService.update(any(), any(), any()) } returns 1

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
            versionName = v1VersionName,
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
            customVersionName = v1VersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessBeforeVersionCreate(ctx, res, setting)

        // 验证：旧版本被重命名为 init-150（使用existingVersion.version作为后缀）
        verify(exactly = 1) {
            resourceService.update(
                transactionContext = any(),
                record = match { it.versionName == "init-150" && it.description != null },
                commonCondition = match {
                    it.projectId == projectId &&
                        it.templateId == templateId &&
                        it.versionName == "init"
                }
            )
        }
    }

    @Test
    @DisplayName("postProcessor: postProcessBeforeVersionCreate 无同名版本时不做重命名")
    fun testPostProcessorBeforeCreateNoRenameWhenNoConflict() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = dslContext

        // v2 不存在同名版本，无需重命名
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
            versionName = v1VersionName,
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
            customVersionName = v1VersionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessBeforeVersionCreate(ctx, res, setting)

        // 验证：没有调用 update（因为无需重命名）
        verify(exactly = 0) { resourceService.update(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: postProcessBeforeVersionCreate v1VersionName为null时直接返回")
    fun testPostProcessorBeforeCreateSkipWhenV1VersionNameIsNull() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = dslContext

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
            versionName = "some-v2-name",
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
            v1VersionName = null, // null，应该直接返回
            customVersionName = "some-v2-name",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessBeforeVersionCreate(ctx, res, setting)

        // 验证：没有调用任何 resourceService 方法
        verify(exactly = 0) { resourceService.getLatestResource(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { resourceService.update(any(), any(), any()) }
    }

    @Test
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 非CREATE_RELEASE时直接返回")
    fun testPostProcessorInTransactionSkipWhenNotReleaseVersion() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

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
            versionAction = PipelineVersionAction.SAVE_DRAFT, // DRAFT 版本，不应该双写
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)

        // 验证：没有调用 v1 双写
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
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        // v1 双写失败
        every {
            v1Dao.createTemplate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()
            )
        } throws RuntimeException("v1 insert failed")

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

        // strictMode 默认为 true，应该抛出异常
        Assertions.assertThrows(RuntimeException::class.java) {
            post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)
        }
    }

    @Test
    @DisplayName("postProcessor: postProcessBeforeVersionCreate 改名冲突时在strictMode下抛异常")
    fun testPostProcessorBeforeCreateRenameConflictThrows() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = dslContext

        // v2 已存在同名版本（version=100）
        val existingVersionMock = mockk<PipelineTemplateResource>(relaxed = true)
        every { existingVersionMock.version } returns 100L
        every {
            resourceService.getLatestResource(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED,
                version = null,
                versionName = v1VersionName,
                includeDelete = false
            )
        } returns existingVersionMock

        // 重命名时抛异常
        every { resourceService.update(any(), any(), any()) } throws RuntimeException("dup key")

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

        // strictMode 默认为 true，应该抛出异常
        Assertions.assertThrows(RuntimeException::class.java) {
            post.postProcessBeforeVersionCreate(ctx, res, setting)
        }
    }

    @Test
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 约束模板存量升级时跳过双写")
    fun testPostProcessorInTransactionSkipConstraintTemplateDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        // v1 已有版本记录（count > 0），存量升级场景
        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        // 约束模板
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
            v1VersionName = v1VersionName,
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)

        // 验证：约束模板存量升级时，跳过双写到 v1
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
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 约束模板首次创建时正常双写")
    fun testPostProcessorInTransactionConstraintTemplateFirstCreateDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        // v1 没有版本记录（count = 0），首次创建场景
        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 0
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(dsl, any(), true) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        // 约束模板
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
            v1VersionName = v1VersionName,
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)

        // 验证：约束模板首次创建时，正常双写到 v1
        verify(exactly = 1) { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) }
        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = dsl,
                projectId = projectId,
                templateId = templateId,
                templateName = "name",
                versionName = v1VersionName,
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
    @DisplayName("postProcessor: postProcessInTransactionVersionCreate 自定义模板始终正常双写")
    fun testPostProcessorInTransactionCustomizeTemplateAlwaysDualWrite() {
        val projectId = "p"
        val templateId = "tpl"
        val userId = "u"
        val v1VersionName = "v1"

        val resourceService: PipelineTemplateResourceService = mockk()
        val v1Dao: TemplateDao = mockk()
        val v1SettingDao: PipelineSettingDao = mockk()
        val dsl: DSLContext = DSL.using(dslContext.configuration())

        // v1 已有版本记录，但自定义模板仍需双写
        every { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) } returns 1
        stubV1CreateTemplate(v1Dao)
        every { v1SettingDao.saveSetting(dsl, any(), true) } returns 1

        val post = PTemplateCompatibilityVersionPostProcessor(
            v2TemplateResourceService = resourceService,
            v1TemplateDao = v1Dao,
            v1TemplateSettingService = v1SettingDao,
            dslContext = dsl
        )

        // 自定义模板
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
            v1VersionName = v1VersionName,
            customVersionName = "v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )

        post.postProcessInTransactionVersionCreate(dsl, ctx, res, setting)

        // 验证：自定义模板即使已有 v1 版本，也继续双写（不受约束模板限制）
        verify(exactly = 1) { v1Dao.countTemplateVersionNum(dsl, projectId, templateId) }
        verify(exactly = 1) {
            v1Dao.createTemplate(
                dslContext = dsl,
                projectId = projectId,
                templateId = templateId,
                templateName = "name",
                versionName = v1VersionName,
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
