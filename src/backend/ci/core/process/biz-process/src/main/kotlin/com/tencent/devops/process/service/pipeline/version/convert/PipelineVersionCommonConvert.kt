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

package com.tencent.devops.process.service.pipeline.version.convert

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.pipeline.PipelineResourceWithoutVersion
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.pipeline.version.PipelineResourceFactory
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.yaml.pojo.YamlVersion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineVersionCommonConvert @Autowired constructor(
    private val pipelineResourceFactory: PipelineResourceFactory,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineInfoService: PipelineInfoService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService
) {

    fun convert(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int?,
        model: Model,
        yaml: String?,
        baseVersion: Int? = null,
        description: String? = null,
        pipelineSettingWithoutVersion: PipelineSetting,
        versionStatus: VersionStatus,
        versionAction: PipelineVersionAction,
        repoHashId: String? = null,
        branchName: String? = null
    ): PipelineVersionCreateContext {
        return if (model.template != null) {
            convertFromTemplate(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                version = version,
                model = model,
                yaml = yaml,
                baseVersion = baseVersion,
                description = description,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                versionStatus = versionStatus,
                versionAction = versionAction,
                repoHashId = repoHashId,
                branchName = branchName
            )
        } else {
            convertFromNonTemplate(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                version = version,
                model = model,
                yaml = yaml,
                baseVersion = baseVersion,
                description = description,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                versionStatus = versionStatus,
                versionAction = versionAction
            )
        }
    }

    private fun convertFromNonTemplate(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int?,
        model: Model,
        yaml: String?,
        baseVersion: Int?,
        description: String?,
        pipelineSettingWithoutVersion: PipelineSetting,
        versionStatus: VersionStatus,
        versionAction: PipelineVersionAction
    ): PipelineVersionCreateContext {
        val pipelineResourceWithoutVersion = PipelineResourceWithoutVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            yaml = yaml,
            yamlVersion = YamlVersion.V3_0.tag,
            creator = userId,
            createTime = LocalDateTime.now(),
            updater = userId,
            updateTime = LocalDateTime.now(),
            status = versionStatus,
            baseVersion = baseVersion,
            branchAction = BranchVersionAction.ACTIVE.takeIf {
                versionStatus == VersionStatus.BRANCH
            },
            description = description
        )

        val pipelineBasicInfo = pipelineResourceFactory.createPipelineBasicInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            pipelineName = pipelineSettingWithoutVersion.pipelineName,
            pipelineDesc = pipelineSettingWithoutVersion.desc,
            pipelineDisable = yaml?.let {
                transferService.loadYaml(it).disablePipeline == true
            }
        )

        val pipelineDialect = pipelineAsCodeService.getPipelineDialect(
            projectId = projectId,
            asCodeSettings = pipelineSettingWithoutVersion.pipelineAsCodeSettings
        )
        val pipelineInfo = pipelineInfoService.getPipelineInfo(projectId = projectId, pipelineId = pipelineId)
        val pipelineModelBasicInfo = pipelineResourceFactory.createPipelineModelBasicInfo(
            model = model,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            create = pipelineInfo == null,
            versionStatus = versionStatus,
            channelCode = channelCode,
            pipelineDialect = pipelineDialect
        )
        return PipelineVersionCreateContext(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            versionAction = versionAction,
            pipelineInfo = pipelineInfo,
            pipelineBasicInfo = pipelineBasicInfo,
            pipelineModelBasicInfo = pipelineModelBasicInfo,
            pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
            pipelineSettingWithoutVersion = pipelineSettingWithoutVersion
        )
    }

    private fun convertFromTemplate(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int?,
        model: Model,
        yaml: String?,
        baseVersion: Int?,
        description: String?,
        pipelineSettingWithoutVersion: PipelineSetting,
        versionStatus: VersionStatus,
        versionAction: PipelineVersionAction,
        repoHashId: String? = null,
        branchName: String? = null
    ): PipelineVersionCreateContext {
        val pipelineBasicInfo = pipelineResourceFactory.createPipelineBasicInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            pipelineName = pipelineSettingWithoutVersion.pipelineName,
            pipelineDesc = pipelineSettingWithoutVersion.desc,
            pipelineDisable = yaml?.let {
                transferService.loadYaml(it).disablePipeline == true
            }
        )

        val templateInstanceBasicInfo = pipelineResourceFactory.createTemplateInstanceBasicInfo(
            projectId = projectId,
            model = model,
            repoHashId = repoHashId,
            branchName = branchName
        )

        val pipelineResourceWithoutVersion = PipelineResourceWithoutVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            model = templateInstanceBasicInfo.instanceModel,
            yaml = yaml,
            yamlVersion = YamlVersion.V3_0.tag,
            creator = userId,
            createTime = LocalDateTime.now(),
            updater = userId,
            updateTime = LocalDateTime.now(),
            status = versionStatus,
            baseVersion = baseVersion,
            branchAction = BranchVersionAction.ACTIVE.takeIf {
                versionStatus == VersionStatus.BRANCH
            },
            description = description
        )

        val pipelineDialect = pipelineAsCodeService.getPipelineDialect(
            projectId = projectId,
            asCodeSettings = pipelineSettingWithoutVersion.pipelineAsCodeSettings
        )
        val pipelineInfo = pipelineInfoService.getPipelineInfo(projectId = projectId, pipelineId = pipelineId)
        val pipelineModelBasicInfo = pipelineResourceFactory.createPipelineModelBasicInfo(
            model = templateInstanceBasicInfo.instanceModel,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            create = pipelineInfo == null,
            versionStatus = versionStatus,
            channelCode = channelCode,
            pipelineDialect = pipelineDialect
        )

        val templateSetting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateInstanceBasicInfo.templateId,
            settingVersion = templateInstanceBasicInfo.templateSettingVersion
        )
        val instanceSetting = TemplateInstanceUtil.instanceSetting(
            setting = pipelineSettingWithoutVersion,
            templateSetting = templateSetting,
            overrideTemplateField = model.overrideTemplateField
        )

        return PipelineVersionCreateContext(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            versionAction = versionAction,
            pipelineInfo = pipelineInfo,
            pipelineBasicInfo = pipelineBasicInfo,
            pipelineModelBasicInfo = pipelineModelBasicInfo,
            pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
            pipelineSettingWithoutVersion = instanceSetting,
            templateInstanceBasicInfo = templateInstanceBasicInfo
        )
    }
}
