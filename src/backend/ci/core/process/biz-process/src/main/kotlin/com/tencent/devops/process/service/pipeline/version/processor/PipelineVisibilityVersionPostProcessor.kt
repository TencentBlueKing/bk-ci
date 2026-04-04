package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.pojo.PipelineVisibilityType
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.PipelineVisibilityService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线创建后,创作流渠道自动将创建者加入可见范围
 */
@Service
class PipelineVisibilityVersionPostProcessor @Autowired constructor(
    private val pipelineVisibilityService: PipelineVisibilityService
) : PipelineVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        with(context) {
            if (pipelineBasicInfo.channelCode != ChannelCode.CREATIVE_STREAM || pipelineInfo != null) return
            pipelineVisibilityService.addVisibility(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                visibilityList = listOf(
                    PipelineVisibility(
                        type = PipelineVisibilityType.USER,
                        scopeId = userId,
                        scopeName = userId
                    )
                )
            )
        }
    }
}
