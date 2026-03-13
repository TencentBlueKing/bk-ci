package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.template.v2.version.processor.PTemplateVersionNameDeduplicatePostProcessor
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class])
class PTemplateVersionNameDeduplicatePostProcessorTest : BkCiAbstractTest() {

    private val resourceService: PipelineTemplateResourceService = mockk(relaxed = true)
    private lateinit var postProcessor: PTemplateVersionNameDeduplicatePostProcessor

    companion object {
        private const val PROJECT_ID = "test-project"
        private const val TEMPLATE_ID = "test-template"
        private const val USER_ID = "test-user"
    }

    @BeforeEach
    fun setUp() {
        postProcessor = PTemplateVersionNameDeduplicatePostProcessor(
            resourceService = resourceService
        )
    }

    @Test
    @DisplayName("存在同名RELEASED版本时触发重命名")
    fun testRenameWhenDuplicateExists() {
        val versionName = "v1.0"
        val (ctx, res, setting) = buildTestData(
            customVersionName = versionName,
            versionAction = PipelineVersionAction.CREATE_RELEASE
        )

        postProcessor.postProcessBeforeVersionCreate(ctx, res, setting)

        verify(exactly = 1) {
            resourceService.renameExistingReleasedVersionIfDuplicate(
                projectId = PROJECT_ID,
                templateId = TEMPLATE_ID,
                versionName = versionName
            )
        }
    }

    @Test
    @DisplayName("customVersionName为null时使用resource.versionName")
    fun testFallbackToResourceVersionName() {
        val (ctx, res, setting) = buildTestData(
            customVersionName = null,
            resourceVersionName = "res-v1",
            versionAction = PipelineVersionAction.CREATE_RELEASE
        )

        postProcessor.postProcessBeforeVersionCreate(ctx, res, setting)

        verify(exactly = 1) {
            resourceService.renameExistingReleasedVersionIfDuplicate(
                projectId = PROJECT_ID,
                templateId = TEMPLATE_ID,
                versionName = "res-v1"
            )
        }
    }

    @Test
    @DisplayName("非CREATE_RELEASE action时跳过")
    fun testSkipWhenNotReleaseVersion() {
        val (ctx, res, setting) = buildTestData(
            customVersionName = "v1.0",
            versionAction = PipelineVersionAction.SAVE_DRAFT
        )

        postProcessor.postProcessBeforeVersionCreate(ctx, res, setting)

        verify(exactly = 0) {
            resourceService.renameExistingReleasedVersionIfDuplicate(
                any(), any(), any(), any()
            )
        }
    }

    @Test
    @DisplayName("RELEASE_DRAFT action时触发重命名")
    fun testTriggerOnReleaseDraft() {
        val versionName = "v2.0"
        val (ctx, res, setting) = buildTestData(
            customVersionName = versionName,
            versionAction = PipelineVersionAction.RELEASE_DRAFT
        )

        postProcessor.postProcessBeforeVersionCreate(ctx, res, setting)

        verify(exactly = 1) {
            resourceService.renameExistingReleasedVersionIfDuplicate(
                projectId = PROJECT_ID,
                templateId = TEMPLATE_ID,
                versionName = versionName
            )
        }
    }

    private fun buildTestData(
        customVersionName: String? = null,
        resourceVersionName: String? = customVersionName,
        versionAction: PipelineVersionAction
    ): Triple<
        PipelineTemplateVersionCreateContext,
        PipelineTemplateResource,
        PipelineSetting
        > {
        val model = Model.defaultModel("name", USER_ID)
        val without = PTemplateResourceWithoutVersion(
            projectId = PROJECT_ID,
            templateId = TEMPLATE_ID,
            type = PipelineTemplateType.PIPELINE,
            model = model,
            yaml = null,
            status = VersionStatus.RELEASED,
            creator = USER_ID
        )
        val only = PTemplateResourceOnlyVersion(
            version = 200L,
            number = 1,
            versionName = resourceVersionName,
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
            projectId = PROJECT_ID,
            pipelineId = TEMPLATE_ID,
            pipelineName = "test",
            desc = "",
            pipelineAsCodeSettings = null,
            creator = USER_ID,
            updater = USER_ID
        )
        val info = PipelineTemplateInfoV2(
            id = TEMPLATE_ID,
            projectId = PROJECT_ID,
            name = "test",
            desc = null,
            mode = TemplateType.CUSTOMIZE,
            type = PipelineTemplateType.PIPELINE,
            enablePac = false,
            latestVersionStatus = VersionStatus.RELEASED,
            creator = USER_ID
        )
        val ctx = PipelineTemplateVersionCreateContext(
            userId = USER_ID,
            projectId = PROJECT_ID,
            templateId = TEMPLATE_ID,
            customVersionName = customVersionName,
            versionAction = versionAction,
            pipelineTemplateInfo = info,
            pTemplateResourceWithoutVersion = without,
            pTemplateSettingWithoutVersion = setting
        )
        return Triple(ctx, res, setting)
    }
}
