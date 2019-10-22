package com.tencent.devops.quality

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class QualityGateOutElementBizPlugin : ElementBizPlugin<QualityGateOutElement> {

    override fun afterCreate(
            element: QualityGateOutElement,
            projectId: String,
            pipelineId: String,
            pipelineName: String,
            userId: String,
            channelCode: ChannelCode
    ) {
    }

    override fun check(element: QualityGateOutElement, appearedCnt: Int) {
    }

    override fun beforeDelete(element: QualityGateOutElement, userId: String, pipelineId: String?) {
    }

    override fun elementClass(): Class<QualityGateOutElement> {
        return QualityGateOutElement::class.java
    }

}