package com.tencent.devops.quality

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class QualityGateInElementBizPlugin : ElementBizPlugin<QualityGateInElement> {

    override fun afterCreate(
            element: QualityGateInElement,
            projectId: String,
            pipelineId: String,
            pipelineName: String,
            userId: String,
            channelCode: ChannelCode
    ) {
    }

    override fun check(element: QualityGateInElement, appearedCnt: Int) {
    }

    override fun beforeDelete(element: QualityGateInElement, userId: String, pipelineId: String?) {
    }

    override fun elementClass(): Class<QualityGateInElement> {
        return QualityGateInElement::class.java
    }

}