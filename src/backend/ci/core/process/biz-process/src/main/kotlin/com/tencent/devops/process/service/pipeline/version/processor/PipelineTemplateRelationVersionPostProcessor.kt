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

package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模板关联关系版本创建后置处理器
 */
@Service
class PipelineTemplateRelationVersionPostProcessor @Autowired constructor(
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val templatePipelineDao: TemplatePipelineDao
) : PipelineVersionCreatePostProcessor {

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) = with(context) {
        templateInstanceBasicInfo?.let {
            if (pipelineInfo == null || pipelineResourceVersion.status == VersionStatus.RELEASED) {
                // 只有在【创建新流水线】或【实例化时】的情况下，T_TEMPLATE_PIPELINE 关联才存储数据
                createOrUpdateRelation(transactionContext)
            }
        } ?: run {
            if (pipelineInfo == null || pipelineResourceVersion.status == VersionStatus.RELEASED) {
                unbindRelation(transactionContext)
            }
        }
    }

    /**
     * 创建/更新模板关联
     */
    private fun PipelineVersionCreateContext.createOrUpdateRelation(
        transactionContext: DSLContext
    ) {
        val pipelineTemplateRelated = pipelineTemplateRelatedService.get(
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
        // 历史原因,如果是研发商店安装的模版,T_TEMPLATE_PIPELINE中的version存储的是原模版的version
        val templateVersion = if (templateInstanceBasicInfo!!.templateMode == TemplateType.CONSTRAINT) {
            templateInstanceBasicInfo.templateSrcTemplateVersion!!
        } else {
            templateInstanceBasicInfo.templateVersion
        }
        if (pipelineTemplateRelated == null) {
            pipelineTemplateRelatedService.createRelation(
                userId = userId,
                projectId = pipelineBasicInfo.projectId,
                pipelineId = pipelineBasicInfo.pipelineId,
                templateId = templateInstanceBasicInfo.templateId,
                instanceType = templateInstanceBasicInfo.instanceType.type,
                buildNo = pipelineModelBasicInfo.buildNo,
                param = pipelineModelBasicInfo.param,
                fixTemplateVersion = templateVersion
            )
        } else {
            templatePipelineDao.update(
                dslContext = transactionContext,
                projectId = pipelineBasicInfo.projectId,
                templateVersion = templateVersion,
                versionName = templateInstanceBasicInfo.templateVersionName ?: "",
                userId = userId,
                instance = TemplateInstanceUpdate(
                    pipelineId = pipelineBasicInfo.pipelineId,
                    pipelineName = pipelineBasicInfo.pipelineName,
                    buildNo = pipelineModelBasicInfo.buildNo,
                    param = pipelineModelBasicInfo.param
                )
            )
        }
    }

    /**
     * 解绑模板关联
     */
    private fun PipelineVersionCreateContext.unbindRelation(
        transactionContext: DSLContext
    ) {
        val pipelineTemplateRelated = pipelineTemplateRelatedService.get(
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT
            )
        )
        pipelineTemplateRelated?.let {
            pipelineTemplateRelatedService.delete(
                transactionContext = transactionContext,
                condition = PipelineTemplateRelatedCommonCondition(
                    projectId = projectId,
                    templateId = pipelineTemplateRelated.templateId,
                    pipelineId = pipelineTemplateRelated.pipelineId
                )
            )
        }
    }
}
