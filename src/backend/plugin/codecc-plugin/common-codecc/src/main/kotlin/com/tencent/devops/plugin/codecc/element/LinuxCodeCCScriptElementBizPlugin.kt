package com.tencent.devops.plugin.codecc.element

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class LinuxCodeCCScriptElementBizPlugin : ElementBizPlugin<LinuxCodeCCScriptElement> {

    override fun afterCreate(
            element: LinuxCodeCCScriptElement,
            projectId: String,
            pipelineId: String,
            pipelineName: String,
            userId: String,
            channelCode: ChannelCode
    ) {
    }

    override fun beforeDelete(element: LinuxCodeCCScriptElement, userId: String, pipelineId: String?) {
    }

    override fun elementClass(): Class<LinuxCodeCCScriptElement> {
        return LinuxCodeCCScriptElement::class.java
    }

    override fun check(element: LinuxCodeCCScriptElement, appearedCnt: Int) {
        if (appearedCnt > 1) {
            throw IllegalArgumentException("只允许一个代码扫描原子")
        }
    }
}